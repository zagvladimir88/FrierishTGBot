package org.zagvladimir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zagvladimir.model.Chat;
import org.zagvladimir.model.Status;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    Boolean existsChatByChatId(Long chatId);
    List<Chat> findAllByStatus(Status status);
}
