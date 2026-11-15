package com.zagvladimir.fraerish.weather;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrecipitationWatchTest {

    private static final long NINE_AM_MINSK = 1794808800L;
    private static final long THREE_HOURS = 3 * 3600L;
    private static final Instant HALF_PAST_NINE = Instant.ofEpochSecond(NINE_AM_MINSK + 1800);
    private static final Duration LOOKAHEAD = Duration.ofHours(3);
    private static final double MIN_CHANCE = 0.5;

    @Test
    void warnsAboutLikelyRainInNextHours() {
        Forecast forecast = forecast(slot(0, "Clouds", 0.1), slot(1, "Rain", 0.7));

        assertThat(PrecipitationWatch.upcoming(forecast, HALF_PAST_NINE, LOOKAHEAD, MIN_CHANCE))
                .contains(new PrecipitationAlert("Минск", "🌧️ Минск: с 12:00 возможен дождь (70%)"));
    }

    @Test
    void saysNowWhenItAlreadyRains() {
        Forecast forecast = forecast(slot(0, "Snow", 0.9), slot(1, "Clouds", 0));

        assertThat(PrecipitationWatch.upcoming(forecast, HALF_PAST_NINE, LOOKAHEAD, MIN_CHANCE))
                .map(PrecipitationAlert::text)
                .contains("❄️ Минск: сейчас возможен снег (90%)");
    }

    @Test
    void warnsAboutThunderstormEvenWithLowChance() {
        Forecast forecast = forecast(slot(0, "Clouds", 0), slot(1, "Thunderstorm", 0.2));

        assertThat(PrecipitationWatch.upcoming(forecast, HALF_PAST_NINE, LOOKAHEAD, MIN_CHANCE))
                .map(PrecipitationAlert::text)
                .contains("⛈️ Минск: с 12:00 возможна гроза (20%)");
    }

    @Test
    void staysQuietForUnlikelyOrDistantRain() {
        assertThat(PrecipitationWatch.upcoming(forecast(slot(0, "Clouds", 0), slot(1, "Rain", 0.3)), HALF_PAST_NINE, LOOKAHEAD, MIN_CHANCE)).isEmpty();
        assertThat(PrecipitationWatch.upcoming(forecast(slot(0, "Clouds", 0), slot(2, "Rain", 0.9)), HALF_PAST_NINE, LOOKAHEAD, MIN_CHANCE)).isEmpty();
        assertThat(PrecipitationWatch.upcoming(forecast(slot(-1, "Rain", 0.9), slot(0, "Clear", 0)), HALF_PAST_NINE, LOOKAHEAD, MIN_CHANCE)).isEmpty();
    }

    private static Forecast forecast(Forecast.Slot... slots) {
        return new Forecast(List.of(slots), new Forecast.City("Минск", 3 * 3600));
    }

    private static Forecast.Slot slot(int index, String condition, double chance) {
        return new Forecast.Slot(NINE_AM_MINSK + index * THREE_HOURS,
                new CurrentWeather.Temperature(1, 1),
                List.of(new CurrentWeather.Condition(condition, condition)),
                chance);
    }
}