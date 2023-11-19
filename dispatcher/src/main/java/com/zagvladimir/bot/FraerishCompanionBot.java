package com.zagvladimir.bot;

import com.zagvladimir.controller.UpdateController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class FraerishCompanionBot extends TelegramLongPollingBot {

    @Value("${bot.name}")
    private String botUserName;
    @Autowired
    private UpdateController updateController;

    public FraerishCompanionBot(@Value("${bot.token}") String botToken) {
        super(botToken);
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update != null) {
            updateController.processUpdate(update);
        } else
            log.info("Error processing update");
    }

    public void sendAnswerMessage(SendMessage message) {
        if (message != null) {
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Error sending message: {}", e.getMessage());
            }
        }
    }
}
