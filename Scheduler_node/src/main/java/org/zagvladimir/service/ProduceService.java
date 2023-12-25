package org.zagvladimir.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.zagvladimir.model.Chat;
import org.zagvladimir.repository.ChatRepository;

import static org.zagvladimir.model.Status.ACTIVE;

@Service
@RequiredArgsConstructor
public class ProduceService {

    private final ChatRepository chatRepository;

    public void processMessage(SendMessage message) {
        Long chatId = Long.valueOf(message.getChatId());
        registerChatIfNotExists(chatId);
    }

    private void registerChatIfNotExists(Long chatId) {
        if (!chatRepository.existsChatByChatId(chatId)) {
            Chat registerChat = createChat(chatId);
            chatRepository.save(registerChat);
        }
    }

    private Chat createChat(Long chatId) {
        return Chat.builder()
                .withChatId(chatId)
                .withStatus(ACTIVE)
                .build();
    }
}