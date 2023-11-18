package org.zagvladimir.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.zagvladimir.model.Chat;
import org.zagvladimir.repository.ChatRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProduceService {

    private final ChatRepository chatRepository;

    public void processMessage(SendMessage message) {
        Long chatId = Long.valueOf(message.getChatId());

        if (!chatRepository.existsChatByChatId(chatId)) {
            Chat registerChat = Chat.builder()
                    .withChatId(Long.valueOf(message.getChatId()))
                    .build();
            chatRepository.save(registerChat);
        }
    }
}