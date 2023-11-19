package com.zagvladimir.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zagvladimir.model.WeatherData;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Service
@RequiredArgsConstructor
public class ProduceService {

    @Value("${api.key}")
    private String apiKey;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public void processMessage(SendMessage message) throws JsonProcessingException {
        if (message != null) {
            String[] messageParts = message.getText().split(" ");

            if (messageParts.length > 1) {
                String url = buildApiUrl(messageParts[1]);

                try {
                    ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
                    WeatherData weatherData = objectMapper.readValue(responseEntity.getBody(), WeatherData.class);
                    SendMessage answer = createWeatherMessage(message, weatherData);
                    produceAnswer(answer);
                } catch (HttpClientErrorException.NotFound ex) {
                    handleCityNotFound(message);
                }
            }
        }
    }

    private void handleCityNotFound(SendMessage message) {
        message.setText("Incorrect city name");
        produceAnswer(message);
    }

    private String buildApiUrl(String cityName) {
        return String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=%s&lang=%s",
                cityName,
                apiKey,
                "metric",
                "ru");
    }

    private SendMessage createWeatherMessage(SendMessage message, WeatherData weather) {
        message.setText(String.format("%s %s %s %nТемпература %s ощущается как %s",
                weather.name(),
                weather.weather().get(0).description(),
                weather.weather().get(0).getEmoji(),
                weather.main().temp(),
                weather.main().feels_like()));
        return message;
    }

    private void produceAnswer(SendMessage sendMessage) {
        rabbitTemplate.convertAndSend("ANSWER", sendMessage);
    }
}
