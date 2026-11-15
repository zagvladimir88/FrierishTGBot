package com.zagvladimir.fraerish.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatRepository {

    private static final String ACTIVE = "ACTIVE";
    private static final String NOT_ACTIVE = "NOT_ACTIVE";

    private final JdbcClient jdbcClient;

    @Transactional
    public void activate(long chatId) {
        if (updateStatus(chatId, ACTIVE) == 0) {
            jdbcClient.sql("INSERT INTO chats (chat_id, status) VALUES (:chatId, :status)")
                    .param("chatId", chatId)
                    .param("status", ACTIVE)
                    .update();
        }
    }

    public void deactivate(long chatId) {
        updateStatus(chatId, NOT_ACTIVE);
    }

    public List<Long> findActiveChatIds() {
        return jdbcClient.sql("SELECT chat_id FROM chats WHERE status = :status ORDER BY id")
                .param("status", ACTIVE)
                .query(Long.class)
                .list();
    }

    private int updateStatus(long chatId, String status) {
        return jdbcClient.sql("UPDATE chats SET status = :status, modification_date = CURRENT_TIMESTAMP WHERE chat_id = :chatId")
                .param("status", status)
                .param("chatId", chatId)
                .update();
    }
}