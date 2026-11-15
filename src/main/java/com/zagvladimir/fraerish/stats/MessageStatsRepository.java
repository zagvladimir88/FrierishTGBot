package com.zagvladimir.fraerish.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageStatsRepository {

    private static final int MAX_NAME_LENGTH = 200;

    private final JdbcClient jdbcClient;

    @Transactional
    public void countMessage(long chatId, long userId, String userName, LocalDate day) {
        String name = userName.length() > MAX_NAME_LENGTH ? userName.substring(0, MAX_NAME_LENGTH) : userName;
        int updated = jdbcClient.sql("""
                        UPDATE message_stats SET messages = messages + 1, user_name = :name
                        WHERE chat_id = :chatId AND user_id = :userId AND stat_day = :day""")
                .param("name", name)
                .param("chatId", chatId)
                .param("userId", userId)
                .param("day", day)
                .update();
        if (updated == 0) {
            jdbcClient.sql("""
                            INSERT INTO message_stats (chat_id, user_id, user_name, stat_day, messages)
                            VALUES (:chatId, :userId, :name, :day, 1)""")
                    .param("chatId", chatId)
                    .param("userId", userId)
                    .param("name", name)
                    .param("day", day)
                    .update();
        }
    }

    public List<UserActivity> top(long chatId, LocalDate from, LocalDate to, int limit) {
        return jdbcClient.sql("""
                        SELECT (SELECT latest.user_name FROM message_stats latest
                                WHERE latest.chat_id = stats.chat_id AND latest.user_id = stats.user_id
                                ORDER BY latest.stat_day DESC LIMIT 1) AS name,
                               SUM(stats.messages) AS total
                        FROM message_stats stats
                        WHERE stats.chat_id = :chatId AND stats.stat_day BETWEEN :from AND :to
                        GROUP BY stats.chat_id, stats.user_id
                        ORDER BY total DESC, name
                        LIMIT :limit""")
                .param("chatId", chatId)
                .param("from", from)
                .param("to", to)
                .param("limit", limit)
                .query((row, number) -> new UserActivity(row.getString("name"), row.getLong("total")))
                .list();
    }

    public long total(long chatId, LocalDate from, LocalDate to) {
        return jdbcClient.sql("SELECT COALESCE(SUM(messages), 0) FROM message_stats WHERE chat_id = :chatId AND stat_day BETWEEN :from AND :to")
                .param("chatId", chatId)
                .param("from", from)
                .param("to", to)
                .query(Long.class)
                .single();
    }

    public int activeUsers(long chatId, LocalDate from, LocalDate to) {
        return jdbcClient.sql("SELECT COUNT(DISTINCT user_id) FROM message_stats WHERE chat_id = :chatId AND stat_day BETWEEN :from AND :to")
                .param("chatId", chatId)
                .param("from", from)
                .param("to", to)
                .query(Integer.class)
                .single();
    }

    public List<Long> chatsWithActivity(LocalDate from, LocalDate to) {
        return jdbcClient.sql("SELECT DISTINCT chat_id FROM message_stats WHERE stat_day BETWEEN :from AND :to ORDER BY chat_id")
                .param("from", from)
                .param("to", to)
                .query(Long.class)
                .list();
    }
}