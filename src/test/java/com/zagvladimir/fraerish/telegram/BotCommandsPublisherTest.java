package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BotCommandsPublisherTest {

    private final TelegramClient telegramClient = mock(TelegramClient.class);
    private final BotCommandsPublisher publisher = new BotCommandsPublisher(
            List.of(handler("/w", "Погода: /w Минск, Брест"), handler("/e", "Курсы: /e USD, EUR, BTC")), telegramClient);

    @Test
    void publishesCommandsInHandlerOrderWithoutSlash() throws TelegramApiException {
        publisher.publish();

        ArgumentCaptor<SetMyCommands> captor = ArgumentCaptor.forClass(SetMyCommands.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getCommands())
                .extracting(BotCommand::getCommand, BotCommand::getDescription)
                .containsExactly(
                        tuple("w", "Погода: /w Минск, Брест"),
                        tuple("e", "Курсы: /e USD, EUR, BTC"));
    }

    @Test
    void keepsWorkingWhenTelegramRejectsCommands() throws TelegramApiException {
        doThrow(new TelegramApiException("network is down")).when(telegramClient).execute(any(SetMyCommands.class));

        assertThatCode(publisher::publish).doesNotThrowAnyException();
    }

    private static CommandHandler handler(String command, String description) {
        return new CommandHandler() {
            @Override
            public String command() {
                return command;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public BotReply handle(CommandRequest request) {
                return BotReply.text(command);
            }
        };
    }
}