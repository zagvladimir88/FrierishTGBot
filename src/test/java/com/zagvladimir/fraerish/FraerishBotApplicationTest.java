package com.zagvladimir.fraerish;

import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@SpringBootTest(properties = {
        "bot.token=test-token",
        "bot.name=TestBot",
        "bot.polling-enabled=false",
        "integration.open-weather.api-key=test",
        "spring.datasource.url=jdbc:h2:mem:fraerish;DB_CLOSE_DELAY=-1"
})
class FraerishBotApplicationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    @Autowired
    private List<CommandHandler> handlers;
    @Autowired
    private ChatRepository chatRepository;

    @Test
    void registersEveryCommandHandler() {
        assertThat(handlers)
                .extracting(CommandHandler::command)
                .containsExactly("/start", "/w", "/e", "/g", "/c", "/s", "/stop");
    }

    @Test
    void createsChatWithDefaultSettingsOnlyOnce() {
        ChatSettings created = chatRepository.findOrCreate(100L);
        chatRepository.findOrCreate(100L);

        assertThat(created).isEqualTo(new ChatSettings(100L, false, LocalTime.of(11, 0), List.of("Минск"), List.of("USD"), true, true, null, true));
    }

    @Test
    void storesSettings() {
        chatRepository.findOrCreate(200L);
        chatRepository.setEnabled(200L, true);
        chatRepository.updateDigestTime(200L, LocalTime.of(8, 30));
        chatRepository.updateCities(200L, List.of("Минск", "Нижний Новгород"));
        chatRepository.updateCurrencies(200L, List.of("USD", "BTC"));
        chatRepository.updateSendRates(200L, false);
        chatRepository.updateWeatherAlerts(200L, false);

        assertThat(chatRepository.find(200L)).contains(new ChatSettings(200L, true, LocalTime.of(8, 30),
                List.of("Минск", "Нижний Новгород"), List.of("USD", "BTC"), false, true, null, false));
    }

    @Test
    void findsEnabledChatsWithWeatherAlertsAndCities() {
        chatRepository.findOrCreate(401L);
        chatRepository.setEnabled(401L, true);
        chatRepository.findOrCreate(402L);
        chatRepository.setEnabled(402L, true);
        chatRepository.updateWeatherAlerts(402L, false);
        chatRepository.findOrCreate(403L);
        chatRepository.findOrCreate(404L);
        chatRepository.setEnabled(404L, true);
        chatRepository.updateCities(404L, List.of());

        assertThat(chatRepository.findWeatherAlertSubscribers())
                .extracting(ChatSettings::chatId)
                .filteredOn(chatId -> chatId > 400L && chatId < 500L)
                .containsExactly(401L);
    }

    @Test
    void findsEnabledChatsWhoseTimeHasComeAndDigestWasNotSentToday() {
        enabledAt(301L, LocalTime.of(8, 0));
        enabledAt(302L, LocalTime.of(9, 0));
        enabledAt(303L, LocalTime.of(7, 0));
        chatRepository.markDigestSent(303L, TODAY);
        enabledAt(304L, LocalTime.of(7, 30));
        chatRepository.markDigestSent(304L, TODAY.minusDays(1));
        chatRepository.findOrCreate(305L);
        chatRepository.updateDigestTime(305L, LocalTime.of(6, 0));

        assertThat(chatRepository.findDue(TODAY, LocalTime.of(8, 30)))
                .extracting(ChatSettings::chatId)
                .filteredOn(chatId -> chatId > 300L)
                .containsExactly(301L, 304L);
    }

    private void enabledAt(long chatId, LocalTime time) {
        chatRepository.findOrCreate(chatId);
        chatRepository.setEnabled(chatId, true);
        chatRepository.updateDigestTime(chatId, time);
    }
}