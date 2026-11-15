package com.zagvladimir.fraerish.digest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

final class DigestTestData {

    static final DigestProperties PROPERTIES = new DigestProperties(ZoneId.of("Europe/Minsk"), "по Минску", Duration.ofMinutes(30), 5, 5);

    private DigestTestData() {
    }

    static ChatSettings chat(long chatId, LocalTime time, List<String> cities, List<String> currencies) {
        return new ChatSettings(chatId, true, time, cities, currencies, true, true, null);
    }

    static ChatSettings chat(long chatId, LocalTime time, List<String> cities, List<String> currencies,
                             boolean sendRates, boolean sendWeather, LocalDate lastDigestDate) {
        return new ChatSettings(chatId, true, time, cities, currencies, sendRates, sendWeather, lastDigestDate);
    }
}