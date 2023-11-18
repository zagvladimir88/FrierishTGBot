package com.zagvladimir.service;

import com.zagvladimir.bot.FraerishCompanionBot;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.zagvladimir.model.RabbitQueue.ANSWER;

@AllArgsConstructor
@Service
public class AnswerConsumer {

    private final FraerishCompanionBot bot;


    @RabbitListener(queues = ANSWER)
    public void consume(SendMessage sendMessage) {
        bot.sendAnswerMessage(sendMessage);
    }
}
