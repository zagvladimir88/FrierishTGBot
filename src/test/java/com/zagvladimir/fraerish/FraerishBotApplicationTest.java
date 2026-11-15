package com.zagvladimir.fraerish;

import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.stats.MessageStatsRepository;
import com.zagvladimir.fraerish.stats.UserActivity;
import com.zagvladimir.fraerish.story.StoryGame;
import com.zagvladimir.fraerish.story.StoryRepository;
import com.zagvladimir.fraerish.story.StoryScore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@Testcontainers
@SpringBootTest(properties = {
        "bot.token=test-token",
        "bot.name=TestBot",
        "bot.polling-enabled=false",
        "integration.open-weather.api-key=test",
        "POSTGRES_PASSWORD=unused"
})
class FraerishBotApplicationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.12-alpine");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    @Autowired
    private List<CommandHandler> handlers;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private MessageStatsRepository messageStatsRepository;
    @Autowired
    private StoryRepository storyRepository;

    @Test
    void registersEveryCommandHandler() {
        assertThat(handlers)
                .extracting(CommandHandler::command)
                .containsExactly("/start", "/w", "/e", "/g", "/c", "/pick", "/poll", "/top", "/story", "/answer", "/story_top", "/story_options", "/s", "/stop");
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

    @Test
    void countsMessagesPerUserAndDay() {
        long group = -900L;
        messageStatsRepository.countMessage(group, 1L, "Vladimir", TODAY);
        messageStatsRepository.countMessage(group, 1L, "Vladimir Z", TODAY);
        messageStatsRepository.countMessage(group, 2L, "Игорь", TODAY.minusDays(1));
        messageStatsRepository.countMessage(group, 2L, "Игорь", TODAY);
        messageStatsRepository.countMessage(group, 2L, "Игорь", TODAY);
        messageStatsRepository.countMessage(group, 3L, "Старый", TODAY.minusDays(10));
        assertThat(messageStatsRepository.activeUsers(group, TODAY.minusDays(29), TODAY)).isEqualTo(3);
        assertThat(messageStatsRepository.activeUsers(group, TODAY.minusDays(6), TODAY)).isEqualTo(2);

        assertThat(messageStatsRepository.top(group, TODAY.minusDays(6), TODAY, 10))
                .containsExactly(new UserActivity("Игорь", 3), new UserActivity("Vladimir Z", 2));
        assertThat(messageStatsRepository.total(group, TODAY.minusDays(6), TODAY)).isEqualTo(5L);
        assertThat(messageStatsRepository.chatsWithActivity(TODAY.minusDays(6), TODAY)).contains(group);
        assertThat(messageStatsRepository.top(group, TODAY.minusDays(30), TODAY, 1)).containsExactly(new UserActivity("Игорь", 3));
    }

    @Test
    void storesStoryGamesVotesScoresAndOptions() {
        long group = -700L;
        List<String> options = List.of("Пейзаж", "Селфи", "Рилс");
        storyRepository.createGame(group, 10, "poll-a", options);
        StoryGame first = storyRepository.findOpenGame(group).orElseThrow();
        storyRepository.saveVote(first.id(), 1L, "Игорь", 0);
        storyRepository.saveVote(first.id(), 1L, "Игорь", 1);
        storyRepository.saveVote(first.id(), 2L, "Аня", 2);
        storyRepository.saveVote(first.id(), 3L, "Передумал", 1);
        storyRepository.removeVote(first.id(), 3L);
        storyRepository.closeGame(first.id(), 1);
        storyRepository.createGame(group, 11, "poll-b", options);
        StoryGame second = storyRepository.findOpenGame(group).orElseThrow();
        storyRepository.saveVote(second.id(), 2L, "Аня", 2);
        storyRepository.closeGame(second.id(), 2);

        assertThat(first.options()).isEqualTo(options);
        assertThat(storyRepository.findByPollId("poll-a")).map(StoryGame::answerIndex).contains(1);
        assertThat(storyRepository.findOpenGame(group)).isEmpty();
        assertThat(storyRepository.votersFor(first.id(), 1)).containsExactly("Игорь");
        assertThat(storyRepository.leaderboard(group, 10)).containsExactly(new StoryScore("Игорь", 1, 1), new StoryScore("Аня", 1, 2));

        storyRepository.createGame(group, 12, "poll-c", options);
        StoryGame third = storyRepository.findOpenGame(group).orElseThrow();
        storyRepository.saveVote(third.id(), 1L, "Игорь", 0);
        storyRepository.saveVote(third.id(), 2L, "Аня", 1);
        storyRepository.startConfirmation(third.id(), 901, 2);
        storyRepository.saveConfirmation(third.id(), 1L, 2);
        storyRepository.saveConfirmation(third.id(), 1L, 1);
        storyRepository.saveConfirmation(third.id(), 2L, 1);
        StoryGame confirming = storyRepository.findOpenGame(group).orElseThrow();
        assertThat(storyRepository.countVotes(third.id())).isEqualTo(2);
        assertThat(confirming.confirmMessageId()).isEqualTo(901);
        assertThat(confirming.votersSnapshot()).isEqualTo(2);
        assertThat(confirming.majority()).isEqualTo(2);
        assertThat(storyRepository.confirmationCounts(third.id())).isEqualTo(Map.of(1, 2L));
        storyRepository.closeGame(third.id(), -1);
        assertThat(storyRepository.findOpenGame(group)).isEmpty();
        assertThat(storyRepository.leaderboard(group, 10)).containsExactly(new StoryScore("Игорь", 1, 1), new StoryScore("Аня", 1, 2));

        assertThat(storyRepository.options(group)).isEmpty();
        storyRepository.saveOptions(group, List.of("Кот", "Пёс"));
        storyRepository.saveOptions(group, List.of("Кот", "Пёс", "Рыба"));
        assertThat(storyRepository.options(group)).contains(List.of("Кот", "Пёс", "Рыба"));
    }

    private void enabledAt(long chatId, LocalTime time) {
        chatRepository.findOrCreate(chatId);
        chatRepository.setEnabled(chatId, true);
        chatRepository.updateDigestTime(chatId, time);
    }
}