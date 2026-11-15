package com.zagvladimir.fraerish.digest;

import com.zagvladimir.fraerish.exchange.Quote;
import com.zagvladimir.fraerish.exchange.RateService;
import com.zagvladimir.fraerish.weather.CityWeather;
import com.zagvladimir.fraerish.weather.WeatherService;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class DigestSources {

    private final RateService rateService;
    private final WeatherService weatherService;
    private final Map<String, Quote> quotes = new HashMap<>();
    private final Map<String, CityWeather> weather = new HashMap<>();

    DigestSources(RateService rateService, WeatherService weatherService) {
        this.rateService = rateService;
        this.weatherService = weatherService;
    }

    Quote quote(String code) {
        return quotes.computeIfAbsent(code, rateService::quote);
    }

    CityWeather weather(String city) {
        return weather.computeIfAbsent(city.toLowerCase(Locale.ROOT), key -> weatherService.lookup(city));
    }
}