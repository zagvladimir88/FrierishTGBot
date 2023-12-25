package com.zagvladimir.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.zagvladimir.model.RabbitQueue.TEXT_CONVERSION_QUEUE;


@Service
@Slf4j
@AllArgsConstructor
public class ConsumerService {


    private final ProduceService produceService;

    @RabbitListener(queues = TEXT_CONVERSION_QUEUE)
    public void handleExchangeUpdate(SendMessage message) {
        produceService.processMessage(message);
    }

}