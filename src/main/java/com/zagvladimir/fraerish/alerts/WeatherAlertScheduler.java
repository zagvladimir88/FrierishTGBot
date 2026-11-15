package com.zagvladimir.fraerish.alerts;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import com.zagvladimir.fraerish.telegram.BotSender;
import com.zagvladimir.fraerish.telegram.SendResult;
import com.zagvladimir.fraerish.weather.PrecipitationAlert;
import com.zagvladimir.fraerish.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherAlertScheduler {

    private final ChatRepository chatRepository;
    private final WeatherService weatherService;
    private final BotSender botSender;
    private final WeatherAlertProperties properties;
    private final DigestProperties digestProperties;
    private final Clock clock;

    private final Set<String> alertedToday = new HashSet<>();
    private LocalDate today;

    @Scheduled(cron = "${bot.weather-alerts.cron}", zone = "${bot.digest.zone}")
    public synchronized void checkWeather() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(digestProperties.zone()));
        if (!properties.isActiveAt(now.toLocalTime())) {
            return;
        }
        startDay(now.toLocalDate());
        Map<String, Optional<PrecipitationAlert>> alertsByCity = new HashMap<>();
        for (ChatSettings chat : chatRepository.findWeatherAlertSubscribers()) {
            List<String> newCities = chat.cities().stream().filter(city -> !alertedToday.contains(key(chat, city))).toList();
            List<String> lines = newCities.stream()
                    .map(city -> alertsByCity.computeIfAbsent(normalized(city), ignored -> weatherService.precipitationAlert(
                            city, now.toInstant(), properties.lookahead(), properties.minChance())))
                    .flatMap(Optional::stream)
                    .map(PrecipitationAlert::text)
                    .toList();
            if (!lines.isEmpty()) {
                deliver(chat, newCities, alertsByCity, String.join("\n", lines));
            }
        }
    }

    private void deliver(ChatSettings chat, List<String> cities, Map<String, Optional<PrecipitationAlert>> alertsByCity, String text) {
        SendResult result = botSender.send(chat.chatId(), BotReply.text(text));
        if (result == SendResult.BLOCKED) {
            chatRepository.setEnabled(chat.chatId(), false);
            return;
        }
        if (result == SendResult.SENT) {
            cities.stream()
                    .filter(city -> alertsByCity.get(normalized(city)).isPresent())
                    .forEach(city -> alertedToday.add(key(chat, city)));
        }
    }

    private void startDay(LocalDate date) {
        if (!date.equals(today)) {
            today = date;
            alertedToday.clear();
        }
    }

    private static String key(ChatSettings chat, String city) {
        return chat.chatId() + ":" + normalized(city);
    }

    private static String normalized(String city) {
        return city.toLowerCase(Locale.ROOT);
    }
}