package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.settings.CallbackOutcome;
import com.zagvladimir.fraerish.settings.SettingsController;
import com.zagvladimir.fraerish.stats.ChatStats;
import com.zagvladimir.fraerish.video.VideoLinkListener;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UpdateDispatcherTest {

    private static final long CHAT_ID = 42L;
    private static final long GROUP_ID = -100500L;
    private static final long USER_ID = 7L;
    private static final String BOT_NAME = "FraerishBot";

    private final List<CommandRequest> weatherRequests = new ArrayList<>();
    private final List<CommandRequest> layoutRequests = new ArrayList<>();
    private final BotSender sender = mock(BotSender.class);
    private final SettingsController settingsController = mock(SettingsController.class);
    private final VideoLinkListener videoLinkListener = mock(VideoLinkListener.class);
    private final ChatStats chatStats = mock(ChatStats.class);
    private final UpdateDispatcher dispatcher = new UpdateDispatcher(
            List.of(
                    new StubHandler("/w", List.of("/weather"), false, request -> {
                        weatherRequests.add(request);
                        return BotReply.text("погода");
                    }),
                    new StubHandler("/c", List.of(), true, request -> {
                        layoutRequests.add(request);
                        return BotReply.text("раскладка");
                    }),
                    new StubHandler("/broken", List.of(), false, request -> {
                        throw new IllegalStateException("boom");
                    })),
            settingsController,
            videoLinkListener,
            chatStats,
            sender,
            new BotProperties("token", BOT_NAME));

    @Test
    void countsGroupMessagesOfPeopleOnly() {
        Message named = message(GROUP_ID, "supergroup", "привет");
        User vladimir = new User(USER_ID, "Vladimir", false);
        vladimir.setLastName("Z");
        named.setFrom(vladimir);
        Message fromBot = message(GROUP_ID, "group", "я бот");
        fromBot.setFrom(bot("OtherBot"));

        dispatcher.consume(update(named));
        dispatcher.consume(update(fromBot));
        dispatcher.consume(update(message(CHAT_ID, "private", "в личке")));
        Update edited = new Update();
        edited.setEditedMessage(message(GROUP_ID, "group", "исправил"));
        dispatcher.consume(edited);

        verify(chatStats).countMessage(GROUP_ID, USER_ID, "Vladimir Z");
        verifyNoMoreInteractions(chatStats);
    }

    @Test
    void handsVideoLinksToVideoListenerInsteadOfSettings() {
        Message link = message(GROUP_ID, "group", "https://www.youtube.com/shorts/ojsibtSeGfw");
        link.setMessageId(15);
        when(videoLinkListener.onText(GROUP_ID, 15, "https://www.youtube.com/shorts/ojsibtSeGfw")).thenReturn(true);

        dispatcher.consume(update(link));

        verify(videoLinkListener).onText(GROUP_ID, 15, "https://www.youtube.com/shorts/ojsibtSeGfw");
        verifyNoInteractions(settingsController, sender);
    }

    @Test
    void routesCommandWithChatContext() {
        dispatcher.consume(update(message(CHAT_ID, "private", "/w Минск")));

        assertThat(weatherRequests).containsExactly(new CommandRequest(new ChatContext(CHAT_ID, USER_ID, false, false), "Минск"));
        verify(sender).send(CHAT_ID, BotReply.text("погода"));
    }

    @Test
    void passesTextOfRepliedMessageToCommand() {
        Message original = message(GROUP_ID, "group", "asdf");
        Message command = message(GROUP_ID, "group", "/c@FraerishBot");
        command.setReplyToMessage(original);

        dispatcher.consume(update(command));

        assertThat(layoutRequests).extracting(CommandRequest::repliedText).containsExactly("asdf");
    }

    @Test
    void marksGroupCommandsAndAnonymousAdmins() {
        Message fromAnonymousAdmin = message(GROUP_ID, "supergroup", "/w@FraerishBot Минск");
        fromAnonymousAdmin.setSenderChat(new Chat(GROUP_ID, "supergroup"));

        dispatcher.consume(update(fromAnonymousAdmin));

        assertThat(weatherRequests).extracting(CommandRequest::context)
                .containsExactly(new ChatContext(GROUP_ID, USER_ID, true, true));
    }

    @Test
    void ignoresUnknownCommandsAndCommandsForOtherBots() {
        dispatcher.consume(update(message(CHAT_ID, "private", "/unknown")));
        dispatcher.consume(update(message(CHAT_ID, "private", "/w@OtherBot Минск")));

        assertThat(weatherRequests).isEmpty();
        verifyNoInteractions(sender, settingsController);
    }

    @Test
    void routesCommandAliases() {
        dispatcher.consume(update(message(CHAT_ID, "private", "/weather Минск")));

        assertThat(weatherRequests).extracting(CommandRequest::args).containsExactly("Минск");
    }

    @Test
    void passesPlainTextToSettingsWhichMayBeWaitingForInput() {
        ChatContext context = new ChatContext(GROUP_ID, USER_ID, true, false);
        BotReply saved = BotReply.text("сохранил");
        when(settingsController.onText(context, "Минск, Брест")).thenReturn(Optional.of(saved));

        dispatcher.consume(update(message(GROUP_ID, "group", "Минск, Брест")));
        dispatcher.consume(update(message(GROUP_ID, "group", "просто болтаем")));

        verify(sender).send(GROUP_ID, saved);
        verify(settingsController).onText(context, "просто болтаем");
        verify(sender, never()).send(anyLong(), eq(BotReply.text("просто болтаем")));
    }

    @Test
    void handlesEditedMessagesOnlyForHandlersThatAcceptThem() {
        Update editedWeather = new Update();
        editedWeather.setEditedMessage(message(CHAT_ID, "private", "/w Минск"));
        Update editedLayout = new Update();
        editedLayout.setEditedMessage(message(CHAT_ID, "private", "/c ghbdtn"));

        dispatcher.consume(editedWeather);
        dispatcher.consume(editedLayout);

        assertThat(weatherRequests).isEmpty();
        assertThat(layoutRequests).extracting(CommandRequest::args).containsExactly("ghbdtn");
    }

    @Test
    void repliesWithServiceUnavailableWhenHandlerFails() {
        dispatcher.consume(update(message(CHAT_ID, "private", "/broken")));

        verify(sender).send(CHAT_ID, BotReply.text(UpdateDispatcher.SERVICE_UNAVAILABLE));
    }

    @Test
    void greetsGroupWhenBotIsAdded() {
        BotReply greeting = BotReply.text("привет");
        when(settingsController.onBotAdded(GROUP_ID)).thenReturn(greeting);
        Message joined = message(GROUP_ID, "group", null);
        joined.setNewChatMembers(List.of(bot(BOT_NAME)));

        dispatcher.consume(update(joined));

        verify(sender).send(GROUP_ID, greeting);
    }

    @Test
    void ignoresOtherNewMembersAndDisablesDigestWhenBotLeaves() {
        Message otherJoined = message(GROUP_ID, "group", null);
        otherJoined.setNewChatMembers(List.of(bot("OtherBot")));
        Message botLeft = message(GROUP_ID, "group", null);
        botLeft.setLeftChatMember(bot(BOT_NAME));

        dispatcher.consume(update(otherJoined));
        dispatcher.consume(update(botLeft));

        verify(settingsController, never()).onBotAdded(anyLong());
        verify(settingsController).onBotRemoved(GROUP_ID);
    }

    @Test
    void passesRepliesToBotMessagesToSettings() {
        Message prompt = message(GROUP_ID, "group", "🏙 Напишите города");
        prompt.setFrom(bot(BOT_NAME));
        Message answer = message(GROUP_ID, "group", "Минск, Брест");
        answer.setReplyToMessage(prompt);
        BotReply saved = BotReply.text("сохранил");
        when(settingsController.onReply(new ChatContext(GROUP_ID, USER_ID, true, false), "🏙 Напишите города", "Минск, Брест"))
                .thenReturn(Optional.of(saved));

        dispatcher.consume(update(answer));

        verify(sender).send(GROUP_ID, saved);
    }

    @Test
    void treatsRepliesToPeopleAsPlainText() {
        Message original = message(GROUP_ID, "group", "кто идёт?");
        Message answer = message(GROUP_ID, "group", "я");
        answer.setReplyToMessage(original);

        dispatcher.consume(update(answer));

        verify(settingsController, never()).onReply(any(), anyString(), anyString());
        verify(settingsController).onText(new ChatContext(GROUP_ID, USER_ID, true, false), "я");
        verifyNoInteractions(sender);
    }

    @Test
    void answersButtonsAndRedrawsMenu() {
        BotReply.Menu menu = new BotReply.Menu("меню", List.of());
        when(settingsController.onButton(new ChatContext(GROUP_ID, USER_ID, true, false), "s:on"))
                .thenReturn(CallbackOutcome.edit(menu, "Сводка включена"));

        dispatcher.consume(buttonPress("s:on"));

        verify(sender).answerButton("query-1", "Сводка включена");
        verify(sender).editMenu(GROUP_ID, 99, menu);
        verify(sender, never()).send(anyLong(), any());
    }

    @Test
    void sendsPromptRequestedByButton() {
        BotReply.Prompt prompt = new BotReply.Prompt("🏙 Напишите города");
        when(settingsController.onButton(any(), anyString())).thenReturn(CallbackOutcome.message(prompt));

        dispatcher.consume(buttonPress("s:cities"));

        verify(sender).answerButton("query-1", null);
        verify(sender).send(GROUP_ID, prompt);
    }

    @Test
    void answersButtonEvenWhenSettingsFail() {
        when(settingsController.onButton(any(), anyString())).thenThrow(new IllegalStateException("db is down"));

        dispatcher.consume(buttonPress("s:on"));

        verify(sender).answerButton("query-1", UpdateDispatcher.SERVICE_UNAVAILABLE);
    }

    private static Update update(Message message) {
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private static Update buttonPress(String data) {
        CallbackQuery query = new CallbackQuery();
        query.setId("query-1");
        query.setFrom(user(USER_ID, false, "someone"));
        query.setData(data);
        Message menuMessage = message(GROUP_ID, "group", "меню");
        menuMessage.setMessageId(99);
        query.setMessage(menuMessage);
        Update update = new Update();
        update.setCallbackQuery(query);
        return update;
    }

    private static Message message(long chatId, String chatType, String text) {
        Message message = new Message();
        message.setChat(new Chat(chatId, chatType));
        message.setFrom(user(USER_ID, false, "someone"));
        message.setText(text);
        return message;
    }

    private static User bot(String userName) {
        return user(1000L, true, userName);
    }

    private static User user(long id, boolean isBot, String userName) {
        User user = new User(id, userName, isBot);
        user.setUserName(userName);
        return user;
    }

    private record StubHandler(String command, List<String> aliases, boolean acceptsEditedMessages,
                               Function<CommandRequest, BotReply> behaviour) implements CommandHandler {

        @Override
        public String description() {
            return command;
        }

        @Override
        public BotReply handle(CommandRequest request) {
            return behaviour.apply(request);
        }
    }
}