package com.zagvladimir.bot;


import com.zagvladimir.service.UpdateProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.zagvladimir.model.RabbitQueue.EXCHANGE;
import static org.zagvladimir.model.RabbitQueue.SCHEDULE;
import static org.zagvladimir.model.RabbitQueue.WEATHER;


@Slf4j
@Component
public class FraerishCompanionBot extends TelegramLongPollingBot {
    public final UpdateProducer updateProducer;

    @Value("${bot.name}")
    private String botUserName;

    public FraerishCompanionBot(@Value("${bot.token}") String botToken, UpdateProducer updateProducer) {
        super(botToken);
        this.updateProducer = updateProducer;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info(update.getMessage().getText());
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        SendMessage sendMessage = new SendMessage();
        var message = update.getMessage();

        if (message.getText().startsWith("/")) {
            String[] parts = message.getText().split(" ");
            String command = parts[0];
            sendMessage.setChatId(message.getChatId());
            sendMessage.setText(message.getText());

            switch (command) {
                case "/start" -> {
                    var userName = update.getMessage().getChat().getUserName();
                    startCommand(message.getChatId(), userName);
                    updateProducer.produce(SCHEDULE, sendMessage);
                }
                case "/w" -> updateProducer.produce(WEATHER, sendMessage);
                case "/exchange" -> updateProducer.produce(EXCHANGE, sendMessage);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    public void sendAnswerMessage(SendMessage message) {
        if (message != null) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }

    private void startCommand(Long chatId, String userName) {
        var text = """
                Добро пожаловать в бот, %s!

                Доступные команды:
                /w - прогноз погоды "/w Лондон"
                """;
        var formattedText = String.format(text, userName);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendAnswerMessage(sendMessage);
    }
}
