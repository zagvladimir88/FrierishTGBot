package com.zagvladimir.fraerish.stats;

import com.zagvladimir.fraerish.digest.DigestProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStats {

    static final int TOP_SIZE = 10;
    private static final List<String> MEDALS = List.of("🥇", "🥈", "🥉");

    private final MessageStatsRepository repository;
    private final DigestProperties digestProperties;
    private final Clock clock;

    public void countMessage(long chatId, long userId, String userName) {
        try {
            repository.countMessage(chatId, userId, userName, today());
        } catch (DataAccessException e) {
            log.warn("Could not count message in chat {}: {}", chatId, e.getMessage());
        }
    }

    public String report(long chatId, int days, String title) {
        LocalDate to = today();
        LocalDate from = to.minusDays(days - 1L);
        List<UserActivity> top = repository.top(chatId, from, to, TOP_SIZE);
        if (top.isEmpty()) {
            return "%s\nПока никто ничего не написал".formatted(title);
        }
        long total = repository.total(chatId, from, to);
        List<String> lines = new ArrayList<>();
        lines.add("%s — %d %s".formatted(title, total, messagesWord(total)));
        for (int i = 0; i < top.size(); i++) {
            String place = i < MEDALS.size() ? MEDALS.get(i) : (i + 1) + ".";
            lines.add("%s %s — %d".formatted(place, top.get(i).name(), top.get(i).messages()));
        }
        return String.join("\n", lines);
    }

    LocalDate today() {
        return LocalDate.now(clock.withZone(digestProperties.zone()));
    }

    static String messagesWord(long count) {
        long lastTwo = count % 100;
        long last = count % 10;
        if (lastTwo >= 11 && lastTwo <= 14) {
            return "сообщений";
        }
        if (last == 1) {
            return "сообщение";
        }
        return last >= 2 && last <= 4 ? "сообщения" : "сообщений";
    }
}