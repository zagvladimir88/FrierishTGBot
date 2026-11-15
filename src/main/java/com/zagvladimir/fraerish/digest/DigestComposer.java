package com.zagvladimir.fraerish.digest;

import com.zagvladimir.fraerish.exchange.Quote;
import com.zagvladimir.fraerish.exchange.QuoteFormatter;
import com.zagvladimir.fraerish.weather.CityWeather;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

final class DigestComposer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"));

    private DigestComposer() {
    }

    static Digest compose(ChatSettings chat, LocalDate date, DigestSources sources) {
        List<String> sections = new ArrayList<>();
        sections.add("%s! %s".formatted(greeting(chat.digestTime()), DATE.format(date)));
        boolean complete = true;
        if (chat.includesRates()) {
            List<Quote> quotes = chat.currencies().stream().map(sources::quote).toList();
            complete = quotes.stream().noneMatch(Quote.Unavailable.class::isInstance);
            sections.add("💱 Курсы\n" + quotes.stream().map(QuoteFormatter::format).collect(Collectors.joining("\n")));
        }
        if (chat.includesWeather()) {
            List<CityWeather> weather = chat.cities().stream().map(sources::weather).toList();
            complete &= weather.stream().noneMatch(CityWeather.Unavailable.class::isInstance);
            sections.add("🌤 Погода\n" + weather.stream().map(CityWeather::text).collect(Collectors.joining("\n\n")));
        }
        return new Digest(String.join("\n\n", sections), complete);
    }

    private static String greeting(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 5 && hour < 12) {
            return "Доброе утро";
        }
        if (hour >= 12 && hour < 17) {
            return "Добрый день";
        }
        if (hour >= 17 && hour < 23) {
            return "Добрый вечер";
        }
        return "Доброй ночи";
    }
}