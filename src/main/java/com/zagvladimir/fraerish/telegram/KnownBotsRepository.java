package com.zagvladimir.fraerish.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KnownBotsRepository {

    private final JdbcClient jdbcClient;

    public void remember(long chatId, long botId) {
        jdbcClient.sql("""
                        INSERT INTO chat_bots (chat_id, bot_id)
                        SELECT :chatId, :botId WHERE NOT EXISTS (SELECT 1 FROM chat_bots WHERE chat_id = :chatId AND bot_id = :botId)""")
                .param("chatId", chatId)
                .param("botId", botId)
                .update();
    }

    public void forget(long chatId, long botId) {
        jdbcClient.sql("DELETE FROM chat_bots WHERE chat_id = :chatId AND bot_id = :botId")
                .param("chatId", chatId)
                .param("botId", botId)
                .update();
    }

    public int count(long chatId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM chat_bots WHERE chat_id = :chatId")
                .param("chatId", chatId)
                .query(Integer.class)
                .single();
    }
}