package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SettingsControllerTest {

    private static final long CHAT_ID = -100L;
    private static final ChatContext GROUP_MEMBER = new ChatContext(CHAT_ID, 5L, true, false);
    private static final ChatSettings DISABLED = new ChatSettings(CHAT_ID, false, LocalTime.of(11, 0), List.of("Минск"), List.of("USD"), true, true, null);
    private static final ChatSettings ENABLED = new ChatSettings(CHAT_ID, true, LocalTime.of(8, 30), List.of("Минск", "Брест"), List.of("USD", "BTC"), true, false, null);

    private final SettingsService settingsService = mock(SettingsService.class);
    private final SettingsAccess settingsAccess = mock(SettingsAccess.class);
    private final SettingsMenu settingsMenu = new SettingsMenu(
            new DigestProperties(ZoneId.of("Europe/Minsk"), "по Минску", Duration.ofMinutes(30), 5, 5));
    private final PendingInputs pendingInputs = new PendingInputs(Clock.fixed(Instant.parse("2026-11-16T08:00:00Z"), ZoneOffset.UTC));
    private final SettingsController controller = new SettingsController(settingsService, settingsMenu, settingsAccess, pendingInputs);

    @BeforeEach
    void memberIsAdmin() {
        when(settingsAccess.canConfigure(any())).thenReturn(true);
    }

    @Test
    void takesCitiesFromNextMessageOfUserWhoPressedButton() {
        BotReply.Prompt prompt = (BotReply.Prompt) controller.onButton(GROUP_MEMBER, SettingsMenu.CITIES).message();
        when(settingsService.updateCities(CHAT_ID, "Минск, Брест, Гродно")).thenReturn(new ListUpdate("Сохранил города: Минск, Брест, Гродно", ENABLED));

        assertThat(prompt.text()).startsWith("🏙 Напишите города через запятую следующим сообщением.");
        assertThat(controller.onText(new ChatContext(CHAT_ID, 6L, true, false), "а я просто пишу")).isEmpty();
        BotReply reply = controller.onText(GROUP_MEMBER, "Минск, Брест, Гродно").orElseThrow();

        assertThat(((BotReply.Menu) reply).text()).startsWith("Сохранил города: Минск, Брест, Гродно");
        assertThat(controller.onText(GROUP_MEMBER, "ещё сообщение")).isEmpty();
    }

    @Test
    void ignoresPlainTextWhenNothingWasAsked() {
        assertThat(controller.onText(GROUP_MEMBER, "Минск")).isEmpty();
        verifyNoInteractions(settingsService);
    }

    @Test
    void greetsGroupWithSettingsMenu() {
        when(settingsService.settings(CHAT_ID)).thenReturn(DISABLED);

        BotReply.Menu menu = (BotReply.Menu) controller.onBotAdded(CHAT_ID);

        assertThat(menu.text()).startsWith(SettingsController.WELCOME).contains("""
                ⚙️ Ежедневная сводка: выключена
                🕘 Время: 11:00 (по Минску)
                🏙 Города: Минск
                💱 Валюты: USD
                📦 В сводке: курсы и погода""");
        assertThat(menu.keyboard().getFirst()).containsExactly(new BotReply.Button("🔔 Включить сводку", SettingsMenu.ENABLE));
    }

    @Test
    void enablesDigestAndRedrawsMenu() {
        when(settingsService.setEnabled(CHAT_ID, true)).thenReturn(ENABLED);

        CallbackOutcome outcome = controller.onButton(GROUP_MEMBER, SettingsMenu.ENABLE);

        assertThat(outcome.toast()).isEqualTo("Сводка включена");
        assertThat(outcome.edit().text()).contains("🕘 Время: 08:30", "🏙 Города: Минск, Брест", "💱 Валюты: USD, BTC", "только курсы");
        assertThat(outcome.edit().keyboard().getFirst()).containsExactly(new BotReply.Button("🔕 Выключить сводку", SettingsMenu.DISABLE));
        assertThat(outcome.edit().keyboard().get(2))
                .containsExactly(new BotReply.Button("✅ Курсы", SettingsMenu.RATES), new BotReply.Button("⬜ Погода", SettingsMenu.WEATHER));
    }

    @Test
    void picksTimeInTwoSteps() {
        CallbackOutcome hours = controller.onButton(GROUP_MEMBER, SettingsMenu.TIME);
        assertThat(hours.edit().keyboard()).hasSize(5);
        assertThat(hours.edit().keyboard().getFirst().getFirst()).isEqualTo(new BotReply.Button("00", "s:h:00"));

        CallbackOutcome minutes = controller.onButton(GROUP_MEMBER, "s:h:08");
        assertThat(minutes.edit().keyboard().getFirst()).extracting(BotReply.Button::data)
                .containsExactly("s:t:08:00", "s:t:08:15", "s:t:08:30", "s:t:08:45");

        when(settingsService.setDigestTime(CHAT_ID, LocalTime.of(8, 30))).thenReturn(ENABLED);
        CallbackOutcome saved = controller.onButton(GROUP_MEMBER, "s:t:08:30");
        assertThat(saved.toast()).isEqualTo("Сводка будет приходить в 08:30");
        verify(settingsService).setDigestTime(CHAT_ID, LocalTime.of(8, 30));
    }

    @Test
    void asksForCitiesAndSavesTheReply() {
        CallbackOutcome outcome = controller.onButton(GROUP_MEMBER, SettingsMenu.CITIES);
        BotReply.Prompt prompt = (BotReply.Prompt) outcome.message();
        when(settingsService.updateCities(CHAT_ID, "минск, брест")).thenReturn(new ListUpdate("Сохранил города: Минск, Брест", ENABLED));

        BotReply reply = controller.onReply(GROUP_MEMBER, prompt.text(), "минск, брест").orElseThrow();

        assertThat(((BotReply.Menu) reply).text()).startsWith("Сохранил города: Минск, Брест\n\n⚙️ Ежедневная сводка: включена");
    }

    @Test
    void savesCurrenciesFromReply() {
        BotReply.Prompt prompt = (BotReply.Prompt) controller.onButton(GROUP_MEMBER, SettingsMenu.CURRENCIES).message();
        when(settingsService.updateCurrencies(CHAT_ID, "usd btc")).thenReturn(new ListUpdate("Сохранил валюты: USD, BTC", ENABLED));

        assertThat(controller.onReply(GROUP_MEMBER, prompt.text(), "usd btc")).isPresent();
        verify(settingsService).updateCurrencies(CHAT_ID, "usd btc");
    }

    @Test
    void ignoresRepliesToOtherBotMessages() {
        assertThat(controller.onReply(GROUP_MEMBER, "Минск ясно", "спасибо")).isEmpty();
        assertThat(controller.onReply(GROUP_MEMBER, null, "спасибо")).isEmpty();
        verifyNoInteractions(settingsService);
    }

    @Test
    void onlyAdminsChangeGroupSettings() {
        when(settingsAccess.canConfigure(GROUP_MEMBER)).thenReturn(false);

        assertThat(controller.onButton(GROUP_MEMBER, SettingsMenu.ENABLE)).isEqualTo(CallbackOutcome.toast(SettingsAccess.ADMINS_ONLY));
        assertThat(controller.onReply(GROUP_MEMBER, SettingsMenu.CITIES_PROMPT, "Минск")).contains(BotReply.text(SettingsAccess.ADMINS_ONLY));
        verifyNoInteractions(settingsService);
    }

    @Test
    void ignoresForeignOrBrokenButtons() {
        assertThat(controller.onButton(GROUP_MEMBER, "other:1")).isEqualTo(CallbackOutcome.toast(null));
        assertThat(controller.onButton(GROUP_MEMBER, "s:h:99")).isEqualTo(CallbackOutcome.toast(null));
        assertThat(controller.onButton(GROUP_MEMBER, "s:t:25:61")).isEqualTo(CallbackOutcome.toast(null));
        assertThat(controller.onButton(GROUP_MEMBER, null)).isEqualTo(CallbackOutcome.toast(null));
        verifyNoInteractions(settingsService);
    }

    @Test
    void disablesDigestWhenBotIsRemoved() {
        controller.onBotRemoved(CHAT_ID);

        verify(settingsService).setEnabled(CHAT_ID, false);
    }
}