package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UpdateDispatcherTest {

    private static final long CHAT_ID = 42L;

    private final List<CommandRequest> weatherRequests = new ArrayList<>();
    private final List<CommandRequest> layoutRequests = new ArrayList<>();
    private final BotSender sender = mock(BotSender.class);
    private final UpdateDispatcher dispatcher = new UpdateDispatcher(
            List.of(
                    new StubHandler("/w", false, request -> {
                        weatherRequests.add(request);
                        return BotReply.text("погода");
                    }),
                    new StubHandler("/c", true, request -> {
                        layoutRequests.add(request);
                        return BotReply.text("раскладка");
                    }),
                    new StubHandler("/broken", false, request -> {
                        throw new IllegalStateException("boom");
                    })),
            sender,
            new BotProperties("token", "FraerishBot", null));

    @Test
    void routesCommandToHandlerAndSendsReply() {
        dispatcher.consume(newMessage("/w Минск"));

        assertThat(weatherRequests).containsExactly(new CommandRequest(CHAT_ID, "Минск"));
        verify(sender).send(CHAT_ID, BotReply.text("погода"));
    }

    @Test
    void routesCommandAddressedToThisBot() {
        dispatcher.consume(newMessage("/w@FraerishBot Минск"));

        assertThat(weatherRequests).containsExactly(new CommandRequest(CHAT_ID, "Минск"));
    }

    @Test
    void ignoresPlainTextUnknownCommandsAndOtherBots() {
        dispatcher.consume(newMessage("привет"));
        dispatcher.consume(newMessage("/unknown"));
        dispatcher.consume(newMessage("/w@OtherBot Минск"));

        assertThat(weatherRequests).isEmpty();
        verify(sender, never()).send(anyLong(), any());
    }

    @Test
    void ignoresMessagesWithoutText() {
        Update update = new Update();
        update.setEditedMessage(message(null));

        dispatcher.consume(update);

        verify(sender, never()).send(anyLong(), any());
    }

    @Test
    void handlesEditedMessagesOnlyForHandlersThatAcceptThem() {
        dispatcher.consume(editedMessage("/w Минск"));
        dispatcher.consume(editedMessage("/c ghbdtn"));

        assertThat(weatherRequests).isEmpty();
        assertThat(layoutRequests).containsExactly(new CommandRequest(CHAT_ID, "ghbdtn"));
    }

    @Test
    void repliesWithServiceUnavailableWhenHandlerFails() {
        dispatcher.consume(newMessage("/broken"));

        verify(sender).send(CHAT_ID, BotReply.text(UpdateDispatcher.SERVICE_UNAVAILABLE));
    }

    private static Update newMessage(String text) {
        Update update = new Update();
        update.setMessage(message(text));
        return update;
    }

    private static Update editedMessage(String text) {
        Update update = new Update();
        update.setEditedMessage(message(text));
        return update;
    }

    private static Message message(String text) {
        Message message = new Message();
        message.setChat(new Chat(CHAT_ID, "private"));
        message.setText(text);
        return message;
    }

    private record StubHandler(String command, boolean acceptsEditedMessages,
                               Function<CommandRequest, BotReply> behaviour) implements CommandHandler {

        @Override
        public BotReply handle(CommandRequest request) {
            return behaviour.apply(request);
        }
    }
}