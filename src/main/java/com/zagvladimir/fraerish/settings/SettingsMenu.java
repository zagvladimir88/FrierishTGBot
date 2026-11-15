package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.BotReply.Button;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class SettingsMenu {

    static final String ENABLE = "s:on";
    static final String DISABLE = "s:off";
    static final String TIME = "s:time";
    static final String HOUR = "s:h:";
    static final String MINUTE = "s:t:";
    static final String CITIES = "s:cities";
    static final String CURRENCIES = "s:cur";
    static final String RATES = "s:rates";
    static final String WEATHER = "s:weather";
    static final String MAIN = "s:menu";

    static final String CITIES_PROMPT = "🏙 Напишите города через запятую";
    static final String CURRENCIES_PROMPT = "💱 Напишите валюты через запятую";

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int HOURS_PER_ROW = 6;
    private static final List<Integer> MINUTES = List.of(0, 15, 30, 45);

    private final DigestProperties properties;

    public BotReply.Menu main(ChatSettings settings) {
        return main(settings, null);
    }

    public BotReply.Menu main(ChatSettings settings, String intro) {
        String summary = summary(settings);
        String text = intro == null ? summary : intro + "\n\n" + summary;
        return new BotReply.Menu(text, List.of(
                List.of(settings.enabled()
                        ? new Button("🔕 Выключить сводку", DISABLE)
                        : new Button("🔔 Включить сводку", ENABLE)),
                List.of(new Button("🕘 Время", TIME), new Button("🏙 Города", CITIES), new Button("💱 Валюты", CURRENCIES)),
                List.of(new Button(check(settings.sendRates()) + " Курсы", RATES),
                        new Button(check(settings.sendWeather()) + " Погода", WEATHER))));
    }

    public BotReply.Menu hours() {
        List<List<Button>> rows = new ArrayList<>();
        for (int start = 0; start < 24; start += HOURS_PER_ROW) {
            rows.add(IntStream.range(start, start + HOURS_PER_ROW)
                    .mapToObj(hour -> new Button("%02d".formatted(hour), HOUR + "%02d".formatted(hour)))
                    .toList());
        }
        rows.add(List.of(new Button("« Назад", MAIN)));
        return new BotReply.Menu("🕘 Во сколько присылать сводку (%s)? Выберите час:".formatted(properties.zoneLabel()), rows);
    }

    public BotReply.Menu minutes(int hour) {
        List<Button> times = MINUTES.stream()
                .map(minute -> LocalTime.of(hour, minute).format(HH_MM))
                .map(time -> new Button(time, MINUTE + time))
                .toList();
        return new BotReply.Menu("🕘 Выберите время:", List.of(times, List.of(new Button("« Назад", TIME))));
    }

    public BotReply.Prompt citiesPrompt() {
        return new BotReply.Prompt(CITIES_PROMPT + " следующим сообщением.\nНапример: Минск, Брест, Нижний Новгород");
    }

    public BotReply.Prompt currenciesPrompt() {
        return new BotReply.Prompt(CURRENCIES_PROMPT + " следующим сообщением.\nНапример: USD, EUR, BTC");
    }

    public Optional<PromptKind> promptKind(String promptText) {
        if (promptText == null) {
            return Optional.empty();
        }
        if (promptText.startsWith(CITIES_PROMPT)) {
            return Optional.of(PromptKind.CITIES);
        }
        if (promptText.startsWith(CURRENCIES_PROMPT)) {
            return Optional.of(PromptKind.CURRENCIES);
        }
        return Optional.empty();
    }

    private String summary(ChatSettings settings) {
        return String.join("\n",
                "⚙️ Ежедневная сводка: " + (settings.enabled() ? "включена" : "выключена"),
                "🕘 Время: %s (%s)".formatted(settings.digestTime().format(HH_MM), properties.zoneLabel()),
                "🏙 Города: " + listOrDash(settings.cities()),
                "💱 Валюты: " + listOrDash(settings.currencies()),
                "📦 В сводке: " + content(settings));
    }

    private static String content(ChatSettings settings) {
        if (settings.sendRates() && settings.sendWeather()) {
            return "курсы и погода";
        }
        if (settings.sendRates()) {
            return "только курсы";
        }
        return settings.sendWeather() ? "только погода" : "ничего — включите курсы или погоду";
    }

    private static String listOrDash(List<String> values) {
        return values.isEmpty() ? "—" : String.join(", ", values);
    }

    private static String check(boolean enabled) {
        return enabled ? "✅" : "⬜";
    }

    public enum PromptKind {
        CITIES,
        CURRENCIES
    }
}