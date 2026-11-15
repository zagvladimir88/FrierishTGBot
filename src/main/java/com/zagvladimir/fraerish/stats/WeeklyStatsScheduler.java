package com.zagvladimir.fraerish.stats;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.telegram.BotSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyStatsScheduler {

    static final int WEEK = 7;

    private final ChatStats chatStats;
    private final MessageStatsRepository repository;
    private final ChatRepository chatRepository;
    private final BotSender botSender;

    @Scheduled(cron = "${bot.stats.weekly-cron}", zone = "${bot.digest.zone}")
    public void publishWeeklyStats() {
        LocalDate to = chatStats.today();
        for (long chatId : repository.chatsWithActivity(to.minusDays(WEEK - 1L), to)) {
            boolean enabled = chatRepository.find(chatId).map(ChatSettings::enabled).orElse(false);
            if (enabled) {
                botSender.send(chatId, BotReply.text(chatStats.report(chatId, WEEK, "📊 Итоги недели")));
            }
        }
    }
}