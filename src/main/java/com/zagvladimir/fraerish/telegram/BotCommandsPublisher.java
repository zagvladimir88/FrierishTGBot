package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bot.polling-enabled", havingValue = "true", matchIfMissing = true)
public class BotCommandsPublisher {

    private final List<CommandHandler> handlers;
    private final TelegramClient telegramClient;

    @EventListener(ApplicationReadyEvent.class)
    public void publish() {
        List<BotCommand> commands = handlers.stream()
                .map(handler -> new BotCommand(handler.command().substring(1), handler.description()))
                .toList();
        try {
            telegramClient.execute(new SetMyCommands(commands));
            log.info("Published bot commands: {}", commands.stream().map(BotCommand::getCommand).toList());
        } catch (TelegramApiException e) {
            log.warn("Could not publish bot commands: {}", e.getMessage());
        }
    }
}