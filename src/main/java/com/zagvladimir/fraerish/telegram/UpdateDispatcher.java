package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.command.ParsedCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UpdateDispatcher implements LongPollingSingleThreadUpdateConsumer {

    static final String SERVICE_UNAVAILABLE = "Сервис временно недоступен, попробуйте позже";

    private final Map<String, CommandHandler> handlers;
    private final BotSender sender;
    private final BotProperties properties;

    public UpdateDispatcher(List<CommandHandler> handlers, BotSender sender, BotProperties properties) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(CommandHandler::command, Function.identity()));
        this.sender = sender;
        this.properties = properties;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            dispatch(update.getMessage(), false);
        } else if (update.hasEditedMessage()) {
            dispatch(update.getEditedMessage(), true);
        }
    }

    private void dispatch(Message message, boolean edited) {
        if (!message.hasText()) {
            return;
        }
        ParsedCommand.parse(message.getText(), properties.name()).ifPresent(command -> {
            CommandHandler handler = handlers.get(command.name());
            if (handler == null || edited && !handler.acceptsEditedMessages()) {
                return;
            }
            CommandRequest request = new CommandRequest(message.getChatId(), command.args());
            sender.send(request.chatId(), handle(handler, request));
        });
    }

    private BotReply handle(CommandHandler handler, CommandRequest request) {
        try {
            return handler.handle(request);
        } catch (RuntimeException e) {
            log.error("Command {} failed for chat {}", handler.command(), request.chatId(), e);
            return BotReply.text(SERVICE_UNAVAILABLE);
        }
    }
}