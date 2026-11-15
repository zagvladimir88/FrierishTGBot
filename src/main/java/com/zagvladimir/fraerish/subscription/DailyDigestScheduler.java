package com.zagvladimir.fraerish.subscription;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.exchange.ExchangeHandler;
import com.zagvladimir.fraerish.telegram.BotProperties;
import com.zagvladimir.fraerish.telegram.BotSender;
import com.zagvladimir.fraerish.telegram.SendResult;
import com.zagvladimir.fraerish.weather.WeatherHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyDigestScheduler {

    private final ChatRepository chatRepository;
    private final ExchangeHandler exchangeHandler;
    private final WeatherHandler weatherHandler;
    private final BotSender botSender;
    private final BotProperties botProperties;

    @Scheduled(cron = "${bot.schedule.exchange-cron}", zone = "${bot.schedule.zone}")
    public void sendExchangeRate() {
        broadcast("exchange rate", () -> exchangeHandler.rate(botProperties.schedule().currency()));
    }

    @Scheduled(cron = "${bot.schedule.weather-cron}", zone = "${bot.schedule.zone}")
    public void sendWeather() {
        broadcast("weather", () -> weatherHandler.forecast(botProperties.schedule().city()));
    }

    void broadcast(String digestName, Supplier<BotReply> replySupplier) {
        List<Long> chatIds = chatRepository.findActiveChatIds();
        if (chatIds.isEmpty()) {
            return;
        }
        BotReply reply;
        try {
            reply = replySupplier.get();
        } catch (RuntimeException e) {
            log.error("Daily {} digest skipped", digestName, e);
            return;
        }
        chatIds.forEach(chatId -> {
            if (botSender.send(chatId, reply) == SendResult.BLOCKED) {
                chatRepository.deactivate(chatId);
            }
        });
    }
}