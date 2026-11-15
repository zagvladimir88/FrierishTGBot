package com.zagvladimir.fraerish.stats;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import com.zagvladimir.fraerish.telegram.BotSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatStatsTest {

    private static final long GROUP = -100L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final DigestProperties DIGEST = new DigestProperties(ZoneId.of("Europe/Minsk"), "по Минску", Duration.ofMinutes(30), 5, 5);

    private final MessageStatsRepository repository = mock(MessageStatsRepository.class);
    private final ChatStats chatStats = new ChatStats(repository, DIGEST,
            Clock.fixed(Instant.parse("2026-11-15T22:30:00Z"), ZoneOffset.UTC));

    @Test
    void countsMessageForMinskDate() {
        chatStats.countMessage(GROUP, 7L, "Vladimir");

        verify(repository).countMessage(GROUP, 7L, "Vladimir", TODAY);
    }

    @Test
    void ignoresDatabaseFailuresWhileCounting() {
        doThrow(new DataAccessResourceFailureException("db is down")).when(repository).countMessage(anyLong(), anyLong(), any(), any());

        assertThatCode(() -> chatStats.countMessage(GROUP, 7L, "Vladimir")).doesNotThrowAnyException();
    }

    @Test
    void reportsTopWithMedalsAndTotal() {
        when(repository.top(GROUP, TODAY.minusDays(6), TODAY, ChatStats.TOP_SIZE)).thenReturn(List.of(
                new UserActivity("Vladimir", 120), new UserActivity("Игорь", 98),
                new UserActivity("Иегова", 50), new UserActivity("Аня", 3)));
        when(repository.total(GROUP, TODAY.minusDays(6), TODAY)).thenReturn(271L);

        assertThat(chatStats.report(GROUP, 7, "📊 Активность за 7 дней")).isEqualTo("""
                📊 Активность за 7 дней — 271 сообщение
                🥇 Vladimir — 120
                🥈 Игорь — 98
                🥉 Иегова — 50
                4. Аня — 3""");
    }

    @Test
    void reportsQuietChat() {
        assertThat(chatStats.report(GROUP, 7, "📊 Итоги недели")).isEqualTo("📊 Итоги недели\nПока никто ничего не написал");
    }

    @ParameterizedTest
    @CsvSource({"1,сообщение", "2,сообщения", "4,сообщения", "5,сообщений", "11,сообщений", "14,сообщений", "21,сообщение", "112,сообщений", "122,сообщения"})
    void pluralizesMessages(long count, String word) {
        assertThat(ChatStats.messagesWord(count)).isEqualTo(word);
    }

    @Test
    void topWorksOnlyInGroupsAndValidatesDays() {
        TopHandler handler = new TopHandler(chatStats);
        ChatContext group = new ChatContext(GROUP, 7L, true, false);

        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(TopHandler.GROUPS_ONLY));
        assertThat(handler.handle(new CommandRequest(group, "много"))).isEqualTo(BotReply.text(TopHandler.USAGE));
        assertThat(handler.handle(new CommandRequest(group, "400"))).isEqualTo(BotReply.text(TopHandler.USAGE));
        assertThat(((BotReply.Text) handler.handle(new CommandRequest(group, "30"))).text()).startsWith("📊 Активность за 30 дней");
        assertThat(((BotReply.Text) handler.handle(new CommandRequest(group, ""))).text()).startsWith("📊 Активность за 7 дней");
        assertThat(((BotReply.Text) handler.handle(new CommandRequest(group, "1"))).text()).startsWith("📊 Активность за сегодня");
    }

    @Test
    void publishesWeeklyStatsOnlyToEnabledChats() {
        ChatRepository chatRepository = mock(ChatRepository.class);
        BotSender sender = mock(BotSender.class);
        when(repository.chatsWithActivity(TODAY.minusDays(6), TODAY)).thenReturn(List.of(GROUP, -200L, -300L));
        when(chatRepository.find(GROUP)).thenReturn(Optional.of(chat(GROUP, true)));
        when(chatRepository.find(-200L)).thenReturn(Optional.of(chat(-200L, false)));
        when(chatRepository.find(-300L)).thenReturn(Optional.empty());

        new WeeklyStatsScheduler(chatStats, repository, chatRepository, sender).publishWeeklyStats();

        verify(sender).send(eq(GROUP), any(BotReply.Text.class));
        verify(sender, never()).send(eq(-200L), any());
        verify(sender, never()).send(eq(-300L), any());
    }

    @Test
    void skipsWeeklyStatsWithoutActivity() {
        BotSender sender = mock(BotSender.class);

        new WeeklyStatsScheduler(chatStats, repository, mock(ChatRepository.class), sender).publishWeeklyStats();

        verifyNoInteractions(sender);
    }

    private static ChatSettings chat(long chatId, boolean enabled) {
        return new ChatSettings(chatId, enabled, LocalTime.of(11, 0), List.of("Минск"), List.of("USD"), true, true, null, true);
    }
}