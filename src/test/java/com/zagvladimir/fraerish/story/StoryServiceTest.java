package com.zagvladimir.fraerish.story;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.settings.SettingsAccess;
import com.zagvladimir.fraerish.telegram.BotSender;
import com.zagvladimir.fraerish.telegram.SentPoll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StoryServiceTest {

    private static final long GROUP = -100L;
    private static final StoryGame OPEN_GAME = new StoryGame(5L, GROUP, 77, "poll-1", StoryService.DEFAULT_OPTIONS, null);

    private final StoryRepository repository = mock(StoryRepository.class);
    private final BotSender sender = mock(BotSender.class);
    private final SettingsAccess access = mock(SettingsAccess.class);
    private final StoryService service = new StoryService(repository, sender, access);

    @Test
    void startsGameWithDefaultOptionsAndRemembersPoll() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.empty());
        when(repository.options(GROUP)).thenReturn(Optional.empty());
        when(sender.sendTrackedPoll(GROUP, new BotReply.Poll(StoryService.QUESTION, StoryService.DEFAULT_OPTIONS)))
                .thenReturn(Optional.of(new SentPoll(77, "poll-1")));

        assertThat(service.start(GROUP)).isEqualTo(BotReply.none());
        verify(repository).createGame(GROUP, 77, "poll-1", StoryService.DEFAULT_OPTIONS);
    }

    @Test
    void usesChatOptionsAndRefusesSecondOpenGame() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.empty());
        when(repository.options(GROUP)).thenReturn(Optional.of(List.of("Кот", "Пёс")));
        when(sender.sendTrackedPoll(any(Long.class), any())).thenReturn(Optional.empty());

        assertThat(service.start(GROUP)).isEqualTo(BotReply.text("Не получилось создать опрос, попробуйте ещё раз"));
        verify(sender).sendTrackedPoll(GROUP, new BotReply.Poll(StoryService.QUESTION, List.of("Кот", "Пёс")));

        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(OPEN_GAME));
        assertThat(((BotReply.Text) service.start(GROUP)).text()).startsWith("Угадайка уже идёт");
    }

    @Test
    void closesGameWithExactOrPrefixAnswerAndNamesWinners() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(OPEN_GAME));
        when(repository.votersFor(5L, 4)).thenReturn(List.of("Аня", "Игорь"));

        BotReply reply = service.answer(GROUP, "дорожка");

        verify(sender).stopPoll(GROUP, 77);
        verify(repository).closeGame(5L, 4);
        assertThat(reply).isEqualTo(BotReply.text("✅ В сторис: Дорожка в качалке\n🎯 Угадали: Аня, Игорь"));
    }

    @Test
    void acceptsYoForE() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(OPEN_GAME));

        assertThat(service.answer(GROUP, "ЗВЕЗДНОЕ НЕБО")).isEqualTo(BotReply.text("✅ В сторис: Звёздное небо\n😅 Никто не угадал"));
    }

    @Test
    void explainsAmbiguousUnknownOrMissingAnswers() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(OPEN_GAME));

        assertThat(((BotReply.Text) service.answer(GROUP, "")).text()).contains("Варианты: Пейзаж, Звёздное небо");
        assertThat(((BotReply.Text) service.answer(GROUP, "собака")).text()).startsWith("Нет такого варианта");
        verify(repository, never()).closeGame(anyLong(), anyInt());

        when(repository.findOpenGame(GROUP)).thenReturn(Optional.empty());
        assertThat(service.answer(GROUP, "селфи")).isEqualTo(BotReply.text("Сейчас нет открытой угадайки. Начать: /story"));
    }

    @Test
    void recordsAndRetractsVotesOnlyForOpenGames() {
        when(repository.findByPollId("poll-1")).thenReturn(Optional.of(OPEN_GAME));
        when(repository.findByPollId("closed")).thenReturn(Optional.of(new StoryGame(6L, GROUP, 78, "closed", List.of("A", "B"), 0)));

        service.vote("poll-1", 7L, "Игорь", List.of(2));
        service.vote("poll-1", 7L, "Игорь", List.of());
        service.vote("closed", 7L, "Игорь", List.of(1));
        service.vote("unknown", 7L, "Игорь", List.of(1));

        verify(repository).saveVote(5L, 7L, "Игорь", 2);
        verify(repository).removeVote(5L, 7L);
        verify(repository, never()).saveVote(6L, 7L, "Игорь", 1);
    }

    @Test
    void showsLeaderboard() {
        when(repository.leaderboard(GROUP, StoryService.TOP_SIZE)).thenReturn(List.of(
                new StoryScore("Игорь", 5, 7), new StoryScore("Аня", 4, 7), new StoryScore("Vladimir", 2, 3), new StoryScore("Иегова", 0, 1)));

        assertThat(service.top(GROUP)).isEqualTo(BotReply.text("""
                🏆 Лучше всех угадывают сторис
                🥇 Игорь — 5 из 7
                🥈 Аня — 4 из 7
                🥉 Vladimir — 2 из 3
                4. Иегова — 0 из 1"""));
    }

    @Test
    void onlyAdminsChangeOptionsWithinLimits() {
        ChatContext admin = new ChatContext(GROUP, 1L, true, false);
        ChatContext member = new ChatContext(GROUP, 2L, true, false);
        when(access.canConfigure(admin)).thenReturn(true);
        when(access.canConfigure(member)).thenReturn(false);
        when(repository.options(GROUP)).thenReturn(Optional.empty());

        assertThat(service.changeOptions(member, List.of("A", "B"))).isEqualTo(BotReply.text("Варианты угадайки меняют только админы группы"));
        assertThat(((BotReply.Text) service.changeOptions(admin, List.of("A"))).text()).startsWith("Нужно от 2 до 12 вариантов");
        assertThat(service.changeOptions(admin, List.of("Кот", "Пёс"))).isEqualTo(BotReply.text("Варианты для /story: Кот, Пёс"));
        verify(repository).saveOptions(GROUP, List.of("Кот", "Пёс"));
    }

    @Test
    void doesNothingForUnknownPollAnswers() {
        when(repository.findByPollId(anyString())).thenReturn(Optional.empty());

        service.vote("other", 1L, "x", List.of(0));

        verifyNoInteractions(sender, access);
    }
}