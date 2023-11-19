package org.zagvladimir.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.zagvladimir.model.RabbitQueue.EXCHANGE_QUEUE;


@Service
@Slf4j
@AllArgsConstructor
public class ConsumerService {


    private final ProduceService produceService;

    @RabbitListener(queues = EXCHANGE_QUEUE)
    public void handleExchangeUpdate(SendMessage message) throws JsonProcessingException {
        produceService.processMessage(message);
    }

}