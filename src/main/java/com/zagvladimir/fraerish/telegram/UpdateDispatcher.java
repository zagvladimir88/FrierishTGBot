package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.command.ParsedCommand;
import com.zagvladimir.fraerish.settings.CallbackOutcome;
import com.zagvladimir.fraerish.settings.SettingsController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class UpdateDispatcher implements LongPollingSingleThreadUpdateConsumer {

    static final String SERVICE_UNAVAILABLE = "Сервис временно недоступен, попробуйте позже";

    private final Map<String, CommandHandler> handlers;
    private final SettingsController settingsController;
    private final BotSender sender;
    private final BotProperties properties;

    public UpdateDispatcher(List<CommandHandler> handlers, SettingsController settingsController,
                            BotSender sender, BotProperties properties) {
        this.handlers = handlers.stream()
                .flatMap(handler -> Stream.concat(Stream.of(handler.command()), handler.aliases().stream())
                        .map(name -> Map.entry(name, handler)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        this.settingsController = settingsController;
        this.sender = sender;
        this.properties = properties;
    }

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            onButton(update.getCallbackQuery());
        } else if (update.hasMessage()) {
            onMessage(update.getMessage(), false);
        } else if (update.hasEditedMessage()) {
            onMessage(update.getEditedMessage(), true);
        }
    }

    private void onMessage(Message message, boolean edited) {
        if (!edited && onMembershipChange(message)) {
            return;
        }
        if (!message.hasText()) {
            return;
        }
        ChatContext context = context(message);
        Optional<ParsedCommand> command = ParsedCommand.parse(message.getText(), properties.name());
        if (command.isPresent()) {
            runCommand(new CommandRequest(context, command.get().args(), repliedText(message)), command.get().name(), edited);
            return;
        }
        if (edited || message.getText().startsWith("/")) {
            return;
        }
        if (isReplyToBot(message)) {
            reply(context, () -> settingsController.onReply(context, message.getReplyToMessage().getText(), message.getText()));
        } else {
            reply(context, () -> settingsController.onText(context, message.getText()));
        }
    }

    private boolean onMembershipChange(Message message) {
        long chatId = message.getChatId();
        List<User> newMembers = message.getNewChatMembers();
        if (newMembers != null && newMembers.stream().anyMatch(this::isThisBot)) {
            reply(context(message), () -> Optional.of(settingsController.onBotAdded(chatId)));
            return true;
        }
        if (message.getLeftChatMember() != null && isThisBot(message.getLeftChatMember())) {
            settingsController.onBotRemoved(chatId);
            return true;
        }
        return false;
    }

    private void runCommand(CommandRequest request, String commandName, boolean edited) {
        CommandHandler handler = handlers.get(commandName);
        if (handler == null || edited && !handler.acceptsEditedMessages()) {
            return;
        }
        reply(request.context(), () -> Optional.of(handler.handle(request)));
    }

    private static String repliedText(Message message) {
        Message original = message.getReplyToMessage();
        if (original == null) {
            return null;
        }
        return original.hasText() ? original.getText() : original.getCaption();
    }

    private void reply(ChatContext context, Supplier<Optional<BotReply>> action) {
        try {
            action.get().ifPresent(reply -> sender.send(context.chatId(), reply));
        } catch (RuntimeException e) {
            log.error("Failed to handle message in chat {}", context.chatId(), e);
            sender.send(context.chatId(), BotReply.text(SERVICE_UNAVAILABLE));
        }
    }

    private void onButton(CallbackQuery query) {
        MaybeInaccessibleMessage message = query.getMessage();
        if (message == null) {
            sender.answerButton(query.getId(), null);
            return;
        }
        ChatContext context = new ChatContext(message.getChatId(), query.getFrom().getId(), isGroup(message.getChat()), false);
        CallbackOutcome outcome;
        try {
            outcome = settingsController.onButton(context, query.getData());
        } catch (RuntimeException e) {
            log.error("Failed to handle button {} in chat {}", query.getData(), context.chatId(), e);
            outcome = CallbackOutcome.toast(SERVICE_UNAVAILABLE);
        }
        sender.answerButton(query.getId(), outcome.toast());
        if (outcome.edit() != null) {
            sender.editMenu(context.chatId(), message.getMessageId(), outcome.edit());
        }
        if (outcome.message() != null) {
            sender.send(context.chatId(), outcome.message());
        }
    }

    private boolean isReplyToBot(Message message) {
        Message original = message.getReplyToMessage();
        return original != null && original.getFrom() != null && isThisBot(original.getFrom());
    }

    private boolean isThisBot(User user) {
        return Boolean.TRUE.equals(user.getIsBot()) && properties.name().equalsIgnoreCase(user.getUserName());
    }

    private static ChatContext context(Message message) {
        long chatId = message.getChatId();
        long userId = message.getFrom() == null ? chatId : message.getFrom().getId();
        boolean anonymousAdmin = message.getSenderChat() != null && message.getSenderChat().getId() == chatId;
        return new ChatContext(chatId, userId, isGroup(message.getChat()), anonymousAdmin);
    }

    private static boolean isGroup(Chat chat) {
        return chat != null && (Boolean.TRUE.equals(chat.isGroupChat()) || Boolean.TRUE.equals(chat.isSuperGroupChat()));
    }
}