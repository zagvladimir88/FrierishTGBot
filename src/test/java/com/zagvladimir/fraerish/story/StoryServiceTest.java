package com.zagvladimir.fraerish.story;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.settings.CallbackOutcome;
import com.zagvladimir.fraerish.settings.SettingsAccess;
import com.zagvladimir.fraerish.telegram.ChatMembers;
import com.zagvladimir.fraerish.telegram.BotSender;
import com.zagvladimir.fraerish.telegram.SentPoll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoryServiceTest {

    private static final long GROUP = -100L;
    private static final ChatContext IGOR = new ChatContext(GROUP, 7L, true, false);
    private static final StoryGame VOTING = new StoryGame(5L, GROUP, 77, "poll-1", StoryService.DEFAULT_OPTIONS, null);
    private static final StoryGame CONFIRMING_THREE = new StoryGame(5L, GROUP, 77, "poll-1", StoryService.DEFAULT_OPTIONS, null, 900, 3);
    private static final int SELFIE = 3;
    private static final int GYM = 2;

    private final StoryRepository repository = mock(StoryRepository.class);
    private final BotSender sender = mock(BotSender.class);
    private final SettingsAccess access = mock(SettingsAccess.class);
    private final ChatMembers chatMembers = mock(ChatMembers.class);
    private final StoryService service = new StoryService(repository, sender, access, chatMembers);

    @BeforeEach
    void everyoneVoted() {
        when(repository.hasVoted(anyLong(), anyLong())).thenReturn(true);
    }

    @Test
    void startsGameWithAllVotedButtonUnderPoll() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.empty());
        when(repository.options(GROUP)).thenReturn(Optional.empty());
        List<List<BotReply.Button>> button = List.of(List.of(new BotReply.Button("🙋 Все проголосовали", StoryService.ALL_VOTED)));
        when(sender.sendTrackedPoll(GROUP, new BotReply.Poll(StoryService.QUESTION, StoryService.DEFAULT_OPTIONS), button))
                .thenReturn(Optional.of(new SentPoll(77, "poll-1")));

        assertThat(service.start(GROUP)).isEqualTo(BotReply.none());
        verify(repository).createGame(GROUP, 77, "poll-1", StoryService.DEFAULT_OPTIONS);
    }

    @Test
    void refusesSecondOpenGame() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(VOTING));

        assertThat(((BotReply.Text) service.start(GROUP)).text()).startsWith("Угадайка уже идёт");
    }

    @Test
    void asksForTruthWhenEveryActiveMemberVoted() {
        when(repository.findByPollId("poll-1")).thenReturn(Optional.of(VOTING));
        when(repository.players(GROUP)).thenReturn(Optional.empty());
        when(chatMembers.people(GROUP)).thenReturn(Optional.of(3));
        when(repository.countVotes(5L)).thenReturn(3);
        when(sender.sendTrackedMenu(eq(GROUP), any())).thenReturn(Optional.of(900));

        service.vote("poll-1", 7L, "Игорь", List.of(SELFIE));

        verify(repository).saveVote(5L, 7L, "Игорь", SELFIE);
        verify(sender).stopPoll(GROUP, 77);
        BotReply.Menu question = sentMenu();
        assertThat(question.text()).contains("Ваш выбор видите только вы", "Ответили: 0 из 3");
        assertThat(question.keyboard()).flatMap(row -> row).extracting(BotReply.Button::text)
                .containsExactlyElementsOf(StoryService.DEFAULT_OPTIONS);
        verify(repository).startConfirmation(5L, 900, 3);
    }

    @Test
    void keepsVotingUntilEveryoneVoted() {
        when(repository.findByPollId("poll-1")).thenReturn(Optional.of(VOTING));
        when(repository.players(GROUP)).thenReturn(Optional.empty());
        when(chatMembers.people(GROUP)).thenReturn(Optional.of(3));
        when(repository.countVotes(5L)).thenReturn(2);

        service.vote("poll-1", 7L, "Игорь", List.of(SELFIE));

        verify(sender, never()).sendTrackedMenu(anyLong(), any());
    }

    @Test
    void usesConfiguredNumberOfPlayersInsteadOfMemberCount() {
        when(repository.findByPollId("poll-1")).thenReturn(Optional.of(VOTING));
        when(repository.players(GROUP)).thenReturn(Optional.of(2));
        when(repository.countVotes(5L)).thenReturn(2);
        when(sender.sendTrackedMenu(eq(GROUP), any())).thenReturn(Optional.of(900));

        service.vote("poll-1", 7L, "Игорь", List.of(SELFIE));

        verify(repository).startConfirmation(5L, 900, 2);
        verify(chatMembers, never()).people(anyLong());
    }

    @Test
    void waitsForButtonWhenMembersCannotBeCounted() {
        when(repository.findByPollId("poll-1")).thenReturn(Optional.of(VOTING));
        when(repository.players(GROUP)).thenReturn(Optional.empty());
        when(chatMembers.people(GROUP)).thenReturn(Optional.empty());
        when(repository.countVotes(5L)).thenReturn(5);

        service.vote("poll-1", 7L, "Игорь", List.of(SELFIE));

        verify(sender, never()).sendTrackedMenu(anyLong(), any());
    }

    @Test
    void adminsSetOrResetNumberOfPlayers() {
        ChatContext admin = new ChatContext(GROUP, 1L, true, false);
        when(access.canConfigure(admin)).thenReturn(true);
        when(access.canConfigure(IGOR)).thenReturn(false);
        when(chatMembers.people(GROUP)).thenReturn(Optional.of(3));

        assertThat(service.changePlayers(IGOR, "3")).isEqualTo(BotReply.text("Число игроков меняют только админы группы"));
        assertThat(service.changePlayers(admin, "3")).isEqualTo(BotReply.text("Угадайка ждёт голосов от 3 игроков"));
        verify(repository).savePlayers(GROUP, 3);
        assertThat(service.changePlayers(admin, "авто"))
                .isEqualTo(BotReply.text("Считаю игроков сам: участники группы без ботов — сейчас 3"));
        verify(repository).savePlayers(GROUP, null);
        assertThat(((BotReply.Text) service.changePlayers(admin, "много")).text()).startsWith("Напишите число игроков");
        assertThat(service.changePlayers(admin, "0")).isEqualTo(BotReply.text("Считаю игроков сам: участники группы без ботов — сейчас 3"));
        assertThat(service.changePlayers(admin, "500")).isEqualTo(BotReply.text("Игроков может быть от 1 до 100"));
    }

    @Test
    void privateChatNeedsOnlyOneVoteAndRetractedVotesAreRemoved() {
        StoryGame privateGame = new StoryGame(6L, 42L, 10, "poll-2", StoryService.DEFAULT_OPTIONS, null);
        when(repository.findByPollId("poll-2")).thenReturn(Optional.of(privateGame));
        when(repository.countVotes(6L)).thenReturn(1);
        when(sender.sendTrackedMenu(eq(42L), any())).thenReturn(Optional.of(11));

        service.vote("poll-2", 42L, "Vladimir", List.of());
        service.vote("poll-2", 42L, "Vladimir", List.of(1));

        verify(repository).removeVote(6L, 42L);
        verify(repository).startConfirmation(6L, 11, 1);
    }

    @Test
    void ignoresVotesAfterTruthQuestionStarted() {
        when(repository.findByPollId("poll-1")).thenReturn(Optional.of(CONFIRMING_THREE));

        service.vote("poll-1", 7L, "Игорь", List.of(1));

        verify(repository, never()).saveVote(anyLong(), anyLong(), any(), anyInt());
    }

    @Test
    void allVotedButtonStartsTruthQuestionOnce() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(VOTING));
        when(repository.countVotes(5L)).thenReturn(0);
        assertThat(service.onButton(IGOR, StoryService.ALL_VOTED)).isEqualTo(CallbackOutcome.toast("Ещё никто не проголосовал"));

        when(repository.countVotes(5L)).thenReturn(2);
        when(sender.sendTrackedMenu(eq(GROUP), any())).thenReturn(Optional.of(900));
        assertThat(service.onButton(IGOR, StoryService.ALL_VOTED)).isEqualTo(CallbackOutcome.toast("Выясняем, что было на самом деле"));
        verify(repository).startConfirmation(5L, 900, 2);

        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(CONFIRMING_THREE));
        assertThat(service.onButton(IGOR, StoryService.ALL_VOTED).toast()).startsWith("Уже выясняем правду");
    }

    @Test
    void keepsEachChoiceSecretUntilDecided() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(CONFIRMING_THREE));
        when(repository.confirmationCounts(5L)).thenReturn(Map.of(GYM, 1L));

        CallbackOutcome outcome = service.onButton(IGOR, StoryService.CONFIRM + GYM);

        verify(repository).saveConfirmation(5L, 7L, GYM);
        assertThat(outcome).isEqualTo(CallbackOutcome.toast("Ваш ответ «Качалка» учтён — остальные его не видят"));
        BotReply.Menu progress = editedMenu();
        assertThat(progress.text()).contains("Ответили: 1 из 3").doesNotContain("Качалка ·");
        assertThat(progress.keyboard()).flatMap(row -> row).extracting(BotReply.Button::text)
                .containsExactlyElementsOf(StoryService.DEFAULT_OPTIONS);
        verify(repository, never()).closeGame(anyLong(), anyInt());
    }

    @Test
    void waitsForEveryoneEvenWhenOptionAlreadyHasMajority() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(CONFIRMING_THREE));
        when(repository.confirmationCounts(5L)).thenReturn(Map.of(GYM, 2L));

        service.onButton(IGOR, StoryService.CONFIRM + GYM);

        verify(repository, never()).closeGame(anyLong(), anyInt());
        assertThat(editedMenu().text()).contains("Ответили: 2 из 3");
    }

    @Test
    void rejectsAnswerFromSomeoneWhoDidNotVote() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(CONFIRMING_THREE));
        when(repository.hasVoted(5L, 7L)).thenReturn(false);

        assertThat(service.onButton(IGOR, StoryService.CONFIRM + GYM).toast()).startsWith("Вы не голосовали");

        verify(repository, never()).saveConfirmation(anyLong(), anyLong(), anyInt());
    }

    @Test
    void decidesByMajorityOnceEveryoneAnswered() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(CONFIRMING_THREE));
        when(repository.confirmationCounts(5L)).thenReturn(Map.of(GYM, 2L, SELFIE, 1L));
        when(repository.votersFor(5L, GYM)).thenReturn(List.of("Игорь"));

        assertThat(service.onButton(IGOR, StoryService.CONFIRM + GYM)).isEqualTo(CallbackOutcome.toast("Все ответили — итог в сообщении"));

        verify(repository).closeGame(5L, GYM);
        BotReply.Menu result = editedMenu();
        assertThat(result.text()).isEqualTo("✅ В сторис: Качалка (так ответили 2 из 3)\n🎯 Угадали: Игорь");
        assertThat(result.keyboard()).isEmpty();
    }

    @Test
    void decidesByMostAnswersWhenEveryoneAnswered() {
        StoryGame confirmingFour = new StoryGame(5L, GROUP, 77, "poll-1", StoryService.DEFAULT_OPTIONS, null, 900, 4);
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(confirmingFour));
        when(repository.confirmationCounts(5L)).thenReturn(Map.of(GYM, 2L, SELFIE, 1L, 0, 1L));

        service.onButton(IGOR, StoryService.CONFIRM + GYM);

        verify(repository).closeGame(5L, GYM);
    }

    @Test
    void callsEveryoneCheatersWhenAllAnsweredDifferently() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(CONFIRMING_THREE));
        when(repository.confirmationCounts(5L)).thenReturn(Map.of(GYM, 1L, SELFIE, 1L, 0, 1L));

        assertThat(service.onButton(IGOR, StoryService.CONFIRM + SELFIE)).isEqualTo(CallbackOutcome.toast("Большинства нет 🤥"));

        verify(repository).closeGame(5L, StoryRepository.NO_ANSWER);
        BotReply.Menu verdict = editedMenu();
        assertThat(verdict.text()).isEqualTo("🤥 Все жулики — каждый голосует за свой вариант\nУгадайка закрыта без ответа");
        assertThat(verdict.keyboard()).isEmpty();
    }

    @Test
    void closesWithoutAnswerOnEvenSplit() {
        StoryGame confirmingFour = new StoryGame(5L, GROUP, 77, "poll-1", StoryService.DEFAULT_OPTIONS, null, 900, 4);
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(confirmingFour));
        when(repository.confirmationCounts(5L)).thenReturn(Map.of(GYM, 2L, SELFIE, 2L));

        service.onButton(IGOR, StoryService.CONFIRM + SELFIE);

        verify(repository).closeGame(5L, StoryRepository.NO_ANSWER);
        assertThat(editedMenu().text()).startsWith("🤥 Голоса разделились поровну — жулики не договорились");
    }

    @Test
    void ignoresButtonsOfFinishedOrBrokenGames() {
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.empty());
        assertThat(service.onButton(IGOR, StoryService.CONFIRM + GYM)).isEqualTo(CallbackOutcome.toast("Эта угадайка уже закончилась"));

        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(CONFIRMING_THREE));
        assertThat(service.onButton(IGOR, StoryService.CONFIRM + "99")).isEqualTo(CallbackOutcome.toast(null));
        assertThat(service.onButton(IGOR, StoryService.CONFIRM + "x")).isEqualTo(CallbackOutcome.toast(null));

        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(VOTING));
        assertThat(service.onButton(IGOR, StoryService.CONFIRM + GYM)).isEqualTo(CallbackOutcome.toast(null));
        verify(repository, never()).saveConfirmation(anyLong(), anyLong(), anyInt());
    }

    @Test
    void onlyAdminsCloseGameManually() {
        ChatContext admin = new ChatContext(GROUP, 1L, true, false);
        when(access.canConfigure(IGOR)).thenReturn(false);
        when(access.canConfigure(admin)).thenReturn(true);
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(VOTING));
        when(repository.votersFor(5L, 4)).thenReturn(List.of());

        assertThat(((BotReply.Text) service.answer(IGOR, "качалка")).text()).startsWith("Правильный ответ выбирает большинство");
        assertThat(service.answer(admin, "дорожка")).isEqualTo(BotReply.text("✅ В сторис: Дорожка в качалке\n😅 Никто не угадал"));
        verify(sender).stopPoll(GROUP, 77);
        verify(repository).closeGame(5L, 4);
    }

    @Test
    void adminAnswerDuringConfirmationRepliesWithResult() {
        ChatContext admin = new ChatContext(GROUP, 1L, true, false);
        when(access.canConfigure(admin)).thenReturn(true);
        when(repository.findOpenGame(GROUP)).thenReturn(Optional.of(CONFIRMING_THREE));
        when(repository.votersFor(5L, 5)).thenReturn(List.of());
        when(repository.confirmationCounts(5L)).thenReturn(Map.of(5, 1L, 1, 1L));
        String result = "✅ В сторис: Рилс (так ответили 1 из 3)\n😅 Никто не угадал";

        assertThat(service.answer(admin, "рилс")).isEqualTo(BotReply.text(result));

        verify(repository).closeGame(5L, 5);
        assertThat(editedMenu().text()).isEqualTo(result);
    }

    @Test
    void showsLeaderboardAndLetsAdminsChangeOptions() {
        when(repository.leaderboard(GROUP, StoryService.TOP_SIZE)).thenReturn(List.of(new StoryScore("Игорь", 5, 7), new StoryScore("Аня", 4, 7)));
        when(access.canConfigure(IGOR)).thenReturn(true);

        assertThat(service.top(GROUP)).isEqualTo(BotReply.text("🏆 Лучше всех угадывают сторис\n🥇 Игорь — 5 из 7\n🥈 Аня — 4 из 7"));
        assertThat(service.changeOptions(IGOR, List.of("Кот", "Пёс"))).isEqualTo(BotReply.text("Варианты для /story: Кот, Пёс"));
        verify(repository).saveOptions(GROUP, List.of("Кот", "Пёс"));
    }

    private BotReply.Menu sentMenu() {
        ArgumentCaptor<BotReply.Menu> captor = ArgumentCaptor.forClass(BotReply.Menu.class);
        verify(sender).sendTrackedMenu(eq(GROUP), captor.capture());
        return captor.getValue();
    }

    private BotReply.Menu editedMenu() {
        ArgumentCaptor<BotReply.Menu> captor = ArgumentCaptor.forClass(BotReply.Menu.class);
        verify(sender).editMenu(eq(GROUP), eq(900), captor.capture());
        return captor.getValue();
    }
}