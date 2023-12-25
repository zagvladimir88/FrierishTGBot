package com.zagvladimir.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zagvladimir.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.zagvladimir.model.RabbitQueue.ANSWER_PHOTO_QUEUE;
import static org.zagvladimir.model.RabbitQueue.ANSWER_TEXT_QUEUE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProduceService {

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.customsearch.cx}")
    private String searchEngineId;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public void processMessage(SendMessage message) {
        if (message == null) {
            return;
        }

        String[] messageParts = message.getText().split(" ");

        if (messageParts.length > 1) {
            String url = buildApiUrl(messageParts[1]);
            try {
                ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
                SearchResult searchResult = objectMapper.readValue(responseEntity.getBody(), SearchResult.class);
                SendMediaGroup sendMediaGroup = createGroupPhotoMessage(message, searchResult);
                producePhotoAnswer(sendMediaGroup);
            } catch (IOException ex) {
                log.error("Error: {}", ex.getMessage());
                handlePhotoNotFound(message);
            }
        } else handlePhotoNotFound(message);
    }

    private SendMediaGroup createGroupPhotoMessage(SendMessage message, SearchResult searchResult) {
        List<InputMedia> photoList = new ArrayList<>();

        for (int i = 1; i <= 3 && i < searchResult.getItems().size(); i++) {
            InputMediaPhoto photo = new InputMediaPhoto(searchResult.getItems().get(i).getLink());
            photoList.add(photo);
        }

        SendMediaGroup sendMediaGroup = new SendMediaGroup();
        sendMediaGroup.setMedias(photoList);
        sendMediaGroup.setChatId(message.getChatId());
        return sendMediaGroup;
    }

    private String buildApiUrl(String query) {
        return String.format("https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&searchType=image",
                apiKey, searchEngineId, query);
    }

    private void handlePhotoNotFound(SendMessage message) {
        message.setText("Incorrect query param");
        produceTextAnswer(message);
    }

    private void producePhotoAnswer(SendMediaGroup sendMediaGroup) {
        rabbitTemplate.convertAndSend(ANSWER_PHOTO_QUEUE, sendMediaGroup);
    }

    private void produceTextAnswer(SendMessage message) {
        rabbitTemplate.convertAndSend(ANSWER_TEXT_QUEUE, message);
    }
}