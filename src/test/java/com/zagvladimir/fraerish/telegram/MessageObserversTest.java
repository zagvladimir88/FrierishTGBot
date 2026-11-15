package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.stats.ChatStats;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MessageObserversTest {

    private static final long GROUP = -100L;
    private static final String THIS_BOT = "FraerishBot";

    private final ChatStats chatStats = mock(ChatStats.class);
    private final TelegramClient telegramClient = mock(TelegramClient.class);
    private final KnownBotsRepository knownBots = mock(KnownBotsRepository.class);
    private final ChatMembers chatMembers = new ChatMembers(telegramClient, knownBots, new BotProperties("token", THIS_BOT));
    private final MessageCounter messageCounter = new MessageCounter(chatStats);

    @Test
    void countsMessagesOfPeopleWithFullName() {
        User vladimir = new User(7L, "Vladimir", false);
        vladimir.setLastName("Z");

        messageCounter.observe(message(vladimir));
        messageCounter.observe(message(new User(8L, "Робот", true)));
        Message anonymousAdmin = message(new User(1087968824L, "Group", true));
        anonymousAdmin.setSenderChat(new Chat(GROUP, "supergroup"));
        messageCounter.observe(anonymousAdmin);

        verify(chatStats).countMessage(GROUP, 7L, "Vladimir Z");
        verifyNoMoreInteractions(chatStats);
    }

    @Test
    void remembersOtherBotsThatWriteOrJoinAndForgetsThoseWhoLeave() {
        User quizBot = bot(500L, "WouldYouRatherBot");
        User newBot = bot(501L, "NewBot");
        Message joined = message(new User(7L, "Vladimir", false));
        joined.setNewChatMembers(List.of(newBot, bot(1L, THIS_BOT)));
        Message left = message(new User(7L, "Vladimir", false));
        left.setLeftChatMember(newBot);

        chatMembers.observe(message(quizBot));
        chatMembers.observe(joined);
        chatMembers.observe(left);
        chatMembers.observe(message(bot(1L, THIS_BOT)));

        verify(knownBots).remember(GROUP, 500L);
        verify(knownBots).remember(GROUP, 501L);
        verify(knownBots).forget(GROUP, 501L);
        verify(knownBots, never()).remember(GROUP, 1L);
    }

    @Test
    void ignoresAnonymousAdminPlaceholderBot() {
        Message anonymousAdmin = message(bot(1087968824L, "GroupAnonymousBot"));
        anonymousAdmin.setSenderChat(new Chat(GROUP, "supergroup"));

        chatMembers.observe(anonymousAdmin);

        verifyNoInteractions(knownBots);
    }

    @Test
    void countsPeopleAsMembersWithoutThisAndKnownBots() throws TelegramApiException {
        when(telegramClient.execute(any(GetChatMemberCount.class))).thenReturn(5);
        when(knownBots.count(GROUP)).thenReturn(1);

        assertThat(chatMembers.people(GROUP)).contains(3);
    }

    @Test
    void reportsUnknownMemberCountWhenTelegramFails() throws TelegramApiException {
        when(telegramClient.execute(any(GetChatMemberCount.class))).thenThrow(new TelegramApiException("network is down"));

        assertThat(chatMembers.people(GROUP)).isEmpty();
        verify(knownBots, never()).count(anyLong());
    }

    private static Message message(User author) {
        Message message = new Message();
        message.setChat(new Chat(GROUP, "supergroup"));
        message.setFrom(author);
        message.setText("текст");
        return message;
    }

    private static User bot(long id, String userName) {
        User bot = new User(id, userName, true);
        bot.setUserName(userName);
        return bot;
    }
}