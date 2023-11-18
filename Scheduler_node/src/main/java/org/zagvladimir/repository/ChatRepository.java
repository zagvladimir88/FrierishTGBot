package org.zagvladimir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zagvladimir.model.Chat;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    Boolean existsChatByChatId(Long chatId);
}
