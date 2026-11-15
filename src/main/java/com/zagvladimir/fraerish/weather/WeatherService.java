package com.zagvladimir.fraerish.weather;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    static final int FORECAST_SLOTS = 4;
    static final int ALERT_SLOTS = 2;

    private static final Map<String, String> EMOJI_BY_CONDITION = Map.of(
            "clear", "☀️",
            "clouds", "☁️",
            "rain", "🌧️",
            "drizzle", "🌦️",
            "thunderstorm", "⛈️",
            "snow", "❄️",
            "mist", "🌫️",
            "fog", "🌫️");

    private final OpenWeatherClient openWeatherClient;

    public CityWeather lookup(String city) {
        return fetchCurrent(city, weather -> describe(city, weather));
    }

    public CityWeather resolve(String city) {
        return fetchCurrent(city, CurrentWeather::name);
    }

    public Optional<PrecipitationAlert> precipitationAlert(String city, Instant now, Duration lookahead, double minChance) {
        try {
            Forecast forecast = openWeatherClient.forecast(city, ALERT_SLOTS);
            return forecast == null ? Optional.empty() : PrecipitationWatch.upcoming(forecast, now, lookahead, minChance);
        } catch (RestClientException e) {
            log.warn("Forecast for {} is unavailable for alerts: {}", city, e.getMessage());
            return Optional.empty();
        }
    }

    private CityWeather fetchCurrent(String city, Function<CurrentWeather, String> text) {
        try {
            CurrentWeather weather = openWeatherClient.currentWeather(city);
            return weather == null
                    ? new CityWeather.Unavailable(city)
                    : new CityWeather.Found(weather.name(), text.apply(weather));
        } catch (HttpClientErrorException.NotFound e) {
            return new CityWeather.NotFound(city);
        } catch (RestClientException e) {
            log.warn("Current weather for {} is unavailable: {}", city, e.getMessage());
            return new CityWeather.Unavailable(city);
        }
    }

    private String describe(String city, CurrentWeather weather) {
        String current = currentLine(weather);
        return outlook(city).map(outlook -> current + "\n" + outlook).orElse(current);
    }

    private Optional<String> outlook(String city) {
        try {
            return ForecastSummary.describe(openWeatherClient.forecast(city, FORECAST_SLOTS));
        } catch (RestClientException e) {
            log.warn("Forecast for {} is unavailable: {}", city, e.getMessage());
            return Optional.empty();
        }
    }

    private static String currentLine(CurrentWeather weather) {
        CurrentWeather.Condition condition = weather.condition();
        String emoji = EMOJI_BY_CONDITION.getOrDefault(condition.main().toLowerCase(Locale.ROOT), "");
        String summary = Stream.of(weather.name(), condition.description(), emoji)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return "%s, %d°C, ощущается как %d°C".formatted(
                summary, Math.round(weather.main().temp()), Math.round(weather.main().feelsLike()));
    }
}