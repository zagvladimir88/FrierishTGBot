package com.zagvladimir.controller;

import com.zagvladimir.bot.FraerishCompanionBot;
import com.zagvladimir.service.UpdateProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.zagvladimir.util.BotCommands.*;
import static org.zagvladimir.model.RabbitQueue.*;

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
                handleCommand(sendMessage);
            }
        } else if (update.hasEditedMessage() && update.getEditedMessage().getText().startsWith("/c")) {
            SendMessage sendMessageForCorrection = createSendMessageForCorrection(update);
            handleCommand(sendMessageForCorrection);
        }
    }

    private void handleCommand(SendMessage sendMessage) {
        String command = getCommand(sendMessage);

        switch (command) {
            case COMMAND_START -> {
                startCommand(sendMessage.getChatId());
                updateProducer.produce(SCHEDULE_QUEUE, sendMessage);
            }
            case COMMAND_WEATHER -> updateProducer.produce(WEATHER_QUEUE, sendMessage);
            case COMMAND_EXCHANGE -> updateProducer.produce(EXCHANGE_QUEUE, sendMessage);
            case COMMAND_GOOGLE_SEARCH -> updateProducer.produce(GOOGLE_SEARCH_QUEUE, sendMessage);
            case COMMAND_TEXT_CONVERSATION -> updateProducer.produce(TEXT_CONVERSION_QUEUE, sendMessage);
        }
    }

    private String getCommand(SendMessage message) {
        String[] parts = message.getText().split(" ");
        return parts[0];
    }

    private SendMessage createSendMessage(Update update) {
        return SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(update.getMessage().getText())
                .build();
    }

    private SendMessage createSendMessageForCorrection(Update update) {
        return SendMessage.builder()
                .chatId(update.getEditedMessage().getChatId())
                .text(update.getEditedMessage().getText())
                .build();
    }

    private void startCommand(String chatId) {
        var text = """
                Добро пожаловать в Fraerish бот!

                Доступные команды:
                /w - прогноз погоды "/w Лондон"
                /exchange - Курс обмена валют BYN "/exchange USD"
                /g - Поиск изображений в Google"
                /с - Исправление неправильной раскладки /c ghbdtn"
                """;
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        fraerishCompanionBot.sendAnswerMessage(sendMessage);
    }
}
