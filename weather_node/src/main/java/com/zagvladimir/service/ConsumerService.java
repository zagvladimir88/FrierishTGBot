package com.zagvladimir.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.zagvladimir.model.RabbitQueue.WEATHER_QUEUE;

@Service
@Slf4j
@AllArgsConstructor
public class ConsumerService {

    private final ProduceService produceService;

    @RabbitListener(queues = WEATHER_QUEUE)
    public void handleWeatherUpdate(SendMessage sendMessage) throws JsonProcessingException {
        produceService.processMessage(sendMessage);
    }
}