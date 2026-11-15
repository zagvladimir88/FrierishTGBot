package com.zagvladimir.fraerish.digest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ChatSettings(
        long chatId,
        boolean enabled,
        LocalTime digestTime,
        List<String> cities,
        List<String> currencies,
        boolean sendRates,
        boolean sendWeather,
        LocalDate lastDigestDate) {

    public ChatSettings {
        cities = List.copyOf(cities);
        currencies = List.copyOf(currencies);
    }

    public boolean includesRates() {
        return sendRates && !currencies.isEmpty();
    }

    public boolean includesWeather() {
        return sendWeather && !cities.isEmpty();
    }

    public boolean hasContent() {
        return includesRates() || includesWeather();
    }
}