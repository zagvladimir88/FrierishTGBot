package com.zagvladimir.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.zagvladimir.model.RabbitQueue.GOOGLE_SEARCH_QUEUE;

@Service
@Slf4j
@AllArgsConstructor
public class ConsumerService {

    private final ProduceService produceService;

    @RabbitListener(queues = GOOGLE_SEARCH_QUEUE)
    public void handleGoogleSearchMessage(SendMessage sendMessage){
        produceService.processMessage(sendMessage);
    }
}