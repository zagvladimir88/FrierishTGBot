package org.zagvladimir.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.zagvladimir.model.Currency;

import static org.zagvladimir.model.RabbitQueue.ANSWER_TEXT_QUEUE;

@Service
@RequiredArgsConstructor
public class ProduceService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public void processMessage(SendMessage message) throws JsonProcessingException {
        String[] messageParts = message.getText().split(" ");

        if (messageParts.length > 1) {
            String curAbbreviation = messageParts[1];
            String url = buildApiUrl(curAbbreviation);

            try {
                Currency curr = getCurrencyFromApi(url);
                SendMessage sendMessage = createMessage(message, curr);
                produceAnswer(sendMessage);
            } catch (HttpClientErrorException.NotFound ex) {
                handleCurrencyNotFound(message);
            }
        }
    }

    private Currency getCurrencyFromApi(String url) throws JsonProcessingException {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        return objectMapper.readValue(responseEntity.getBody(), Currency.class);
    }

    private void handleCurrencyNotFound(SendMessage message) {
        message.setText("Incorrect currency id");
        produceAnswer(message);
    }

    private String buildApiUrl(String curAbbreviation) {
        return String.format("https://api.nbrb.by/exrates/rates/%s?parammode=2", curAbbreviation);
    }

    private SendMessage createMessage(SendMessage message, Currency curr) {
        message.setText(String.format("Курс %s %s - %s BYN",
                curr.Cur_Scale(),
                curr.Cur_Name(),
                curr.Cur_OfficialRate()));
        return message;
    }

    private void produceAnswer(SendMessage sendMessage) {
        rabbitTemplate.convertAndSend(ANSWER_TEXT_QUEUE, sendMessage);
    }
}