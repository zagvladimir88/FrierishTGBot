package com.zagvladimir.service;

import com.zagvladimir.bot.FraerishCompanionBot;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.zagvladimir.model.RabbitQueue.ANSWER_PHOTO_QUEUE;
import static org.zagvladimir.model.RabbitQueue.ANSWER_TEXT_QUEUE;

@AllArgsConstructor
@Service
public class AnswerConsumer {

    private final FraerishCompanionBot bot;


    @RabbitListener(queues = ANSWER_TEXT_QUEUE)
    public void consumeText(SendMessage sendMessage) {
        bot.sendAnswerMessage(sendMessage);
    }

    @RabbitListener(queues = ANSWER_PHOTO_QUEUE)
    public void consumePhoto(SendMediaGroup sendPhoto) {
        bot.sendImage(sendPhoto);
    }
}
