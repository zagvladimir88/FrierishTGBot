package com.zagvladimir.fraerish.digest;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRepository {

    private static final String ACTIVE = "ACTIVE";
    private static final String NOT_ACTIVE = "NOT_ACTIVE";
    private static final String LIST_SEPARATOR = ",";

    private final JdbcClient jdbcClient;

    public Optional<ChatSettings> find(long chatId) {
        return jdbcClient.sql("SELECT * FROM chats WHERE chat_id = :chatId")
                .param("chatId", chatId)
                .query(ChatRepository::map)
                .optional();
    }

    @Transactional
    public ChatSettings findOrCreate(long chatId) {
        jdbcClient.sql("""
                        INSERT INTO chats (chat_id, status)
                        SELECT :chatId, :status WHERE NOT EXISTS (SELECT 1 FROM chats WHERE chat_id = :chatId)""")
                .param("chatId", chatId)
                .param("status", NOT_ACTIVE)
                .update();
        return find(chatId).orElseThrow();
    }

    public List<ChatSettings> findDue(LocalDate today, LocalTime now) {
        return jdbcClient.sql("""
                        SELECT * FROM chats
                        WHERE status = :status
                          AND digest_time <= :now
                          AND (last_digest_date IS NULL OR last_digest_date < :today)
                        ORDER BY id""")
                .param("status", ACTIVE)
                .param("now", now)
                .param("today", today)
                .query(ChatRepository::map)
                .list();
    }

    public List<ChatSettings> findWeatherAlertSubscribers() {
        return jdbcClient.sql("SELECT * FROM chats WHERE status = :status AND weather_alerts = TRUE ORDER BY id")
                .param("status", ACTIVE)
                .query(ChatRepository::map)
                .list()
                .stream()
                .filter(ChatSettings::receivesWeatherAlerts)
                .toList();
    }

    public void setEnabled(long chatId, boolean enabled) {
        update(chatId, "status = :value", enabled ? ACTIVE : NOT_ACTIVE);
    }

    public void updateDigestTime(long chatId, LocalTime time) {
        update(chatId, "digest_time = :value", time);
    }

    public void updateCities(long chatId, List<String> cities) {
        update(chatId, "cities = :value", String.join(LIST_SEPARATOR, cities));
    }

    public void updateCurrencies(long chatId, List<String> currencies) {
        update(chatId, "currencies = :value", String.join(LIST_SEPARATOR, currencies));
    }

    public void updateSendRates(long chatId, boolean sendRates) {
        update(chatId, "send_rates = :value", sendRates);
    }

    public void updateSendWeather(long chatId, boolean sendWeather) {
        update(chatId, "send_weather = :value", sendWeather);
    }

    public void updateWeatherAlerts(long chatId, boolean weatherAlerts) {
        update(chatId, "weather_alerts = :value", weatherAlerts);
    }

    public void markDigestSent(long chatId, LocalDate date) {
        update(chatId, "last_digest_date = :value", date);
    }

    private void update(long chatId, String assignment, Object value) {
        jdbcClient.sql("UPDATE chats SET " + assignment + ", modification_date = CURRENT_TIMESTAMP WHERE chat_id = :chatId")
                .param("value", value)
                .param("chatId", chatId)
                .update();
    }

    private static ChatSettings map(ResultSet row, int rowNumber) throws SQLException {
        return new ChatSettings(
                row.getLong("chat_id"),
                ACTIVE.equals(row.getString("status")),
                row.getObject("digest_time", LocalTime.class),
                split(row.getString("cities")),
                split(row.getString("currencies")),
                row.getBoolean("send_rates"),
                row.getBoolean("send_weather"),
                row.getObject("last_digest_date", LocalDate.class),
                row.getBoolean("weather_alerts"));
    }

    private static List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.asList(value.split(LIST_SEPARATOR));
    }
}