package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.ListInput;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import com.zagvladimir.fraerish.exchange.Quote;
import com.zagvladimir.fraerish.exchange.RateService;
import com.zagvladimir.fraerish.weather.CityWeather;
import com.zagvladimir.fraerish.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final ChatRepository chatRepository;
    private final WeatherService weatherService;
    private final RateService rateService;
    private final DigestProperties properties;

    public ChatSettings settings(long chatId) {
        return chatRepository.findOrCreate(chatId);
    }

    public ChatSettings setEnabled(long chatId, boolean enabled) {
        chatRepository.findOrCreate(chatId);
        chatRepository.setEnabled(chatId, enabled);
        return settings(chatId);
    }

    public ChatSettings setDigestTime(long chatId, LocalTime time) {
        chatRepository.findOrCreate(chatId);
        chatRepository.updateDigestTime(chatId, time);
        return settings(chatId);
    }

    public ChatSettings toggleRates(long chatId) {
        chatRepository.updateSendRates(chatId, !settings(chatId).sendRates());
        return settings(chatId);
    }

    public ChatSettings toggleWeather(long chatId) {
        chatRepository.updateSendWeather(chatId, !settings(chatId).sendWeather());
        return settings(chatId);
    }

    public ChatSettings toggleWeatherAlerts(long chatId) {
        chatRepository.updateWeatherAlerts(chatId, !settings(chatId).weatherAlerts());
        return settings(chatId);
    }

    public ListUpdate updateCities(long chatId, String input) {
        List<String> requested = ListInput.cities(input);
        if (requested.isEmpty()) {
            return unchanged(chatId, "Не увидел ни одного города. Например: Минск, Брест");
        }
        if (requested.size() > properties.maxCities()) {
            return unchanged(chatId, "Можно указать до %d городов".formatted(properties.maxCities()));
        }
        List<CityWeather> checks = requested.stream().map(weatherService::resolve).toList();
        if (checks.stream().anyMatch(CityWeather.Unavailable.class::isInstance)) {
            return unchanged(chatId, "Сервис погоды сейчас недоступен, попробуйте позже");
        }
        List<String> found = ListInput.distinctIgnoringCase(checks.stream()
                .filter(CityWeather.Found.class::isInstance)
                .map(check -> ((CityWeather.Found) check).name())
                .toList());
        List<String> missing = checks.stream()
                .filter(CityWeather.NotFound.class::isInstance)
                .map(check -> ((CityWeather.NotFound) check).query())
                .toList();
        if (found.isEmpty()) {
            return unchanged(chatId, "Не нашёл такие города: " + String.join(", ", missing));
        }
        chatRepository.updateCities(chatId, found);
        return new ListUpdate(saved("города", found, missing), settings(chatId));
    }

    public ListUpdate updateCurrencies(long chatId, String input) {
        List<String> requested = ListInput.currencies(input);
        if (requested.isEmpty()) {
            return unchanged(chatId, "Не увидел ни одной валюты. Например: USD, EUR, BTC");
        }
        if (requested.size() > properties.maxCurrencies()) {
            return unchanged(chatId, "Можно указать до %d валют".formatted(properties.maxCurrencies()));
        }
        List<Quote> quotes = requested.stream().map(rateService::quote).toList();
        if (quotes.stream().anyMatch(Quote.Unavailable.class::isInstance)) {
            return unchanged(chatId, "Курсы сейчас недоступны, попробуйте позже");
        }
        List<String> found = quotes.stream().filter(Quote::known).map(Quote::code).toList();
        List<String> missing = quotes.stream().filter(quote -> !quote.known()).map(Quote::code).toList();
        if (found.isEmpty()) {
            return unchanged(chatId, "Не знаю такие валюты: " + String.join(", ", missing));
        }
        chatRepository.updateCurrencies(chatId, found);
        return new ListUpdate(saved("валюты", found, missing), settings(chatId));
    }

    private ListUpdate unchanged(long chatId, String message) {
        return new ListUpdate(message, settings(chatId));
    }

    private static String saved(String what, List<String> found, List<String> missing) {
        String message = "Сохранил %s: %s".formatted(what, String.join(", ", found));
        return missing.isEmpty() ? message : message + "\nНе нашёл: " + String.join(", ", missing);
    }
}