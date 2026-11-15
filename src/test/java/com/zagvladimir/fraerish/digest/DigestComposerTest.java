package com.zagvladimir.fraerish.digest;

import com.zagvladimir.fraerish.exchange.Quote;
import com.zagvladimir.fraerish.exchange.RateService;
import com.zagvladimir.fraerish.weather.CityWeather;
import com.zagvladimir.fraerish.weather.WeatherService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.zagvladimir.fraerish.digest.DigestTestData.chat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DigestComposerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);

    private final RateService rateService = mock(RateService.class);
    private final WeatherService weatherService = mock(WeatherService.class);
    private final DigestSources sources = new DigestSources(rateService, weatherService);

    @Test
    void composesRatesAndWeatherSections() {
        when(rateService.quote("USD")).thenReturn(new Quote.Fiat("USD", 1, new BigDecimal("3.0026")));
        when(rateService.quote("BTC")).thenReturn(new Quote.Crypto("BTC", new BigDecimal("65080"), new BigDecimal("195413")));
        when(weatherService.lookup("Минск")).thenReturn(new CityWeather.Found("Минск", "Минск ясно ☀️, 1°C, ощущается как -4°C\nближайшие 12 ч: от 1 до 4°C, без осадков"));
        when(weatherService.lookup("Брест")).thenReturn(new CityWeather.Found("Брест", "Брест облачно ☁️, 3°C, ощущается как 0°C"));

        Digest digest = DigestComposer.compose(chat(1, LocalTime.of(8, 30), List.of("Минск", "Брест"), List.of("USD", "BTC")), DATE, sources);

        assertThat(digest.complete()).isTrue();
        assertThat(digest.text()).isEqualTo("""
                Доброе утро! 16 ноября

                💱 Курсы
                USD — 3.00 BYN
                BTC — 65 080 $ (≈ 195 413 BYN)

                🌤 Погода
                Минск ясно ☀️, 1°C, ощущается как -4°C
                ближайшие 12 ч: от 1 до 4°C, без осадков

                Брест облачно ☁️, 3°C, ощущается как 0°C""");
    }

    @Test
    void leavesOutDisabledSectionsAndGreetsByTimeOfDay() {
        when(weatherService.lookup("Минск")).thenReturn(new CityWeather.Found("Минск", "Минск ясно"));

        Digest digest = DigestComposer.compose(
                chat(1, LocalTime.of(19, 0), List.of("Минск"), List.of("USD"), false, true, null), DATE, sources);

        assertThat(digest.text()).isEqualTo("Добрый вечер! 16 ноября\n\n🌤 Погода\nМинск ясно");
        verifyNoInteractions(rateService);
    }

    @Test
    void marksDigestIncompleteWhenSourceIsUnavailable() {
        when(rateService.quote("USD")).thenReturn(new Quote.Unavailable("USD"));
        when(weatherService.lookup(anyString())).thenReturn(new CityWeather.Found("Минск", "Минск ясно"));

        Digest digest = DigestComposer.compose(chat(1, LocalTime.of(13, 0), List.of("Минск"), List.of("USD")), DATE, sources);

        assertThat(digest.complete()).isFalse();
        assertThat(digest.text()).startsWith("Добрый день! 16 ноября").contains("USD — курс сейчас недоступен");
    }

    @Test
    void unknownCityDoesNotBlockDigest() {
        when(weatherService.lookup("Абырвалг")).thenReturn(new CityWeather.NotFound("Абырвалг"));

        Digest digest = DigestComposer.compose(
                chat(1, LocalTime.of(2, 0), List.of("Абырвалг"), List.of(), false, true, null), DATE, sources);

        assertThat(digest.complete()).isTrue();
        assertThat(digest.text()).isEqualTo("Доброй ночи! 16 ноября\n\n🌤 Погода\nАбырвалг — не нашёл такой город");
    }
}