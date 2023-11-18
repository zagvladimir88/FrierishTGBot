package com.zagvladimir.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.zagvladimir.model.RabbitQueue.WEATHER;

@Service
@Slf4j
@AllArgsConstructor
public class ConsumerService {

    private final ProduceService produceService;

    @RabbitListener(queues = WEATHER)
    public void handleWeatherUpdate(SendMessage sendMessage) throws JsonProcessingException {
        produceService.sendWeather(sendMessage);
    }
}