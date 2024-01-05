package com.zagvladimir.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zagvladimir.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.List;

import static java.util.stream.Collectors.*;
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

    private static final String GOOGLE_COMMAND = "/g";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public void processMessage(SendMessage message) {
        if (message == null) {
            return;
        }

        String queryWithoutCommand = StringUtils.difference(GOOGLE_COMMAND, message.getText());

        if (StringUtils.isNotEmpty(queryWithoutCommand)) {
            String url = buildApiUrl(queryWithoutCommand);

            try {
                SearchResult searchResult = fetchSearchResult(url);
                SendMediaGroup sendMediaGroup = createGroupPhotoMessage(message, searchResult);
                producePhotoAnswer(sendMediaGroup);
            } catch (IOException ex) {
                handlePhotoNotFound(message);
            }
        } else {
            handlePhotoNotFound(message);
        }
    }

    private SearchResult fetchSearchResult(String url) throws IOException {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        return objectMapper.readValue(responseEntity.getBody(), SearchResult.class);
    }

    private SendMediaGroup createGroupPhotoMessage(SendMessage message, SearchResult searchResult) {
        List<InputMedia> photoList = searchResult.getItems()
                .stream()
                .map(item -> new InputMediaPhoto(item.getLink()))
                .collect(toList());

        return SendMediaGroup.builder()
                .medias(photoList)
                .chatId(message.getChatId())
                .build();
    }

    private String buildApiUrl(String query) {
        return String.format("https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&searchType=image&num=3",
                apiKey, searchEngineId, query.trim());
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
