package com.zagvladimir.fraerish.digest;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.exchange.RateService;
import com.zagvladimir.fraerish.telegram.BotSender;
import com.zagvladimir.fraerish.telegram.SendResult;
import com.zagvladimir.fraerish.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DigestScheduler {

    private final ChatRepository chatRepository;
    private final RateService rateService;
    private final WeatherService weatherService;
    private final BotSender botSender;
    private final DigestProperties properties;
    private final Clock clock;

    @Scheduled(cron = "0 * * * * *", zone = "${bot.digest.zone}")
    public void sendDueDigests() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(properties.zone()));
        List<ChatSettings> due = chatRepository.findDue(now.toLocalDate(), now.toLocalTime()).stream()
                .filter(ChatSettings::hasContent)
                .toList();
        if (due.isEmpty()) {
            return;
        }
        DigestSources sources = new DigestSources(rateService, weatherService);
        due.forEach(chat -> deliver(chat, now, sources));
    }

    private void deliver(ChatSettings chat, ZonedDateTime now, DigestSources sources) {
        try {
            LocalDate today = now.toLocalDate();
            boolean canRetry = Duration.between(chat.digestTime(), now.toLocalTime()).compareTo(properties.retryWindow()) < 0;
            Digest digest = DigestComposer.compose(chat, today, sources);
            if (!digest.complete() && canRetry) {
                return;
            }
            SendResult result = botSender.send(chat.chatId(), BotReply.text(digest.text()));
            switch (result) {
                case SENT -> chatRepository.markDigestSent(chat.chatId(), today);
                case BLOCKED -> chatRepository.setEnabled(chat.chatId(), false);
                case FAILED -> {
                    if (!canRetry) {
                        chatRepository.markDigestSent(chat.chatId(), today);
                    }
                }
            }
        } catch (RuntimeException e) {
            log.error("Digest for chat {} failed", chat.chatId(), e);
        }
    }
}