package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.digest.ChatSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SettingsController {

    static final String WELCOME = """
            Привет! Я могу каждый день присылать сюда сводку: курсы валют и погоду.
            Настраивают сводку админы группы — кнопками ниже.""";

    private final SettingsService settingsService;
    private final SettingsMenu settingsMenu;
    private final SettingsAccess settingsAccess;
    private final PendingInputs pendingInputs;

    public BotReply onBotAdded(long chatId) {
        return settingsMenu.main(settingsService.settings(chatId), WELCOME);
    }

    public void onBotRemoved(long chatId) {
        settingsService.setEnabled(chatId, false);
    }

    public CallbackOutcome onButton(ChatContext context, String data) {
        if (data == null || !data.startsWith("s:")) {
            return CallbackOutcome.toast(null);
        }
        if (!settingsAccess.canConfigure(context)) {
            return CallbackOutcome.toast(SettingsAccess.ADMINS_ONLY);
        }
        long chatId = context.chatId();
        return switch (data) {
            case SettingsMenu.ENABLE -> CallbackOutcome.edit(settingsMenu.main(settingsService.setEnabled(chatId, true)), "Сводка включена");
            case SettingsMenu.DISABLE -> CallbackOutcome.edit(settingsMenu.main(settingsService.setEnabled(chatId, false)), "Сводка выключена");
            case SettingsMenu.TIME -> CallbackOutcome.edit(settingsMenu.hours());
            case SettingsMenu.CITIES -> askFor(context, SettingsMenu.PromptKind.CITIES, settingsMenu.citiesPrompt());
            case SettingsMenu.CURRENCIES -> askFor(context, SettingsMenu.PromptKind.CURRENCIES, settingsMenu.currenciesPrompt());
            case SettingsMenu.RATES -> CallbackOutcome.edit(settingsMenu.main(settingsService.toggleRates(chatId)));
            case SettingsMenu.WEATHER -> CallbackOutcome.edit(settingsMenu.main(settingsService.toggleWeather(chatId)));
            case SettingsMenu.ALERTS -> CallbackOutcome.edit(settingsMenu.main(settingsService.toggleWeatherAlerts(chatId)));
            case SettingsMenu.MAIN -> CallbackOutcome.edit(settingsMenu.main(settingsService.settings(chatId)));
            default -> timeSelection(chatId, data);
        };
    }

    public Optional<BotReply> onReply(ChatContext context, String promptText, String text) {
        return settingsMenu.promptKind(promptText).map(kind -> {
            pendingInputs.take(context);
            return apply(context, kind, text);
        });
    }

    public Optional<BotReply> onText(ChatContext context, String text) {
        return pendingInputs.take(context).map(kind -> apply(context, kind, text));
    }

    private CallbackOutcome askFor(ChatContext context, SettingsMenu.PromptKind kind, BotReply.Prompt prompt) {
        pendingInputs.await(context, kind);
        return CallbackOutcome.message(prompt);
    }

    private BotReply apply(ChatContext context, SettingsMenu.PromptKind kind, String text) {
        if (!settingsAccess.canConfigure(context)) {
            return BotReply.text(SettingsAccess.ADMINS_ONLY);
        }
        ListUpdate update = switch (kind) {
            case CITIES -> settingsService.updateCities(context.chatId(), text);
            case CURRENCIES -> settingsService.updateCurrencies(context.chatId(), text);
        };
        return settingsMenu.main(update.settings(), update.message());
    }

    private CallbackOutcome timeSelection(long chatId, String data) {
        try {
            if (data.startsWith(SettingsMenu.HOUR)) {
                int hour = Integer.parseInt(data.substring(SettingsMenu.HOUR.length()));
                return hour >= 0 && hour < 24 ? CallbackOutcome.edit(settingsMenu.minutes(hour)) : CallbackOutcome.toast(null);
            }
            if (data.startsWith(SettingsMenu.MINUTE)) {
                LocalTime time = LocalTime.parse(data.substring(SettingsMenu.MINUTE.length()));
                ChatSettings settings = settingsService.setDigestTime(chatId, time);
                return CallbackOutcome.edit(settingsMenu.main(settings), "Сводка будет приходить в " + data.substring(SettingsMenu.MINUTE.length()));
            }
        } catch (NumberFormatException | DateTimeParseException e) {
            return CallbackOutcome.toast(null);
        }
        return CallbackOutcome.toast(null);
    }
}