package com.zagvladimir.controller;

import com.zagvladimir.bot.FraerishCompanionBot;
import com.zagvladimir.service.UpdateProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.zagvladimir.util.BotCommands.COMMAND_EXCHANGE;
import static com.zagvladimir.util.BotCommands.COMMAND_GOOGLE_SEARCH;
import static com.zagvladimir.util.BotCommands.COMMAND_START;
import static com.zagvladimir.util.BotCommands.COMMAND_TEXT_CONVERSATION;
import static com.zagvladimir.util.BotCommands.COMMAND_WEATHER;
import static org.zagvladimir.model.RabbitQueue.EXCHANGE_QUEUE;
import static org.zagvladimir.model.RabbitQueue.GOOGLE_SEARCH_QUEUE;
import static org.zagvladimir.model.RabbitQueue.SCHEDULE_QUEUE;
import static org.zagvladimir.model.RabbitQueue.TEXT_CONVERSION_QUEUE;
import static org.zagvladimir.model.RabbitQueue.WEATHER_QUEUE;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpdateController {

    private final UpdateProducer updateProducer;
    private final FraerishCompanionBot fraerishCompanionBot;

    public void processUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {

            SendMessage sendMessage = createSendMessage(update);

            if (sendMessage.getText().startsWith("/")) {
                String command = handleCommand(sendMessage);

                switch (command) {
                    case COMMAND_START -> {
                        var userName = update.getMessage().getChat().getUserName();
                        startCommand(sendMessage.getChatId(), userName);
                        updateProducer.produce(SCHEDULE_QUEUE, sendMessage);
                    }
                    case COMMAND_WEATHER -> updateProducer.produce(WEATHER_QUEUE, sendMessage);
                    case COMMAND_EXCHANGE -> updateProducer.produce(EXCHANGE_QUEUE, sendMessage);
                    case COMMAND_GOOGLE_SEARCH -> updateProducer.produce(GOOGLE_SEARCH_QUEUE, sendMessage);
                    case COMMAND_TEXT_CONVERSATION -> updateProducer.produce(TEXT_CONVERSION_QUEUE, sendMessage);
                }
            }
        }
    }

    private SendMessage createSendMessage(Update update) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(update.getMessage().getText())
                .build();
    }

    private String handleCommand(SendMessage message) {
        String[] parts = message.getText().split(" ");
        return parts[0];
    }

    private void startCommand(String chatId, String userName) {
        var text = """
                Добро пожаловать в бот, %s!

                Доступные команды:
                /w - прогноз погоды "/w Лондон"
                /exchange - Курс обмена валют BYN "/exchange USD"
                /g - Поиск изображений в Google"
                """;
        var formattedText = String.format(text, userName);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(formattedText);
        fraerishCompanionBot.sendAnswerMessage(sendMessage);
    }
}
