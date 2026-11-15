package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import com.zagvladimir.fraerish.exchange.Quote;
import com.zagvladimir.fraerish.exchange.RateService;
import com.zagvladimir.fraerish.weather.CityWeather;
import com.zagvladimir.fraerish.weather.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsServiceTest {

    private static final long CHAT_ID = 7L;
    private static final ChatSettings DEFAULTS = new ChatSettings(CHAT_ID, false, LocalTime.of(11, 0), List.of("Минск"), List.of("USD"), true, true, null, true);
    private static final DigestProperties PROPERTIES = new DigestProperties(ZoneId.of("Europe/Minsk"), "по Минску", Duration.ofMinutes(30), 3, 3);

    private final ChatRepository chatRepository = mock(ChatRepository.class);
    private final WeatherService weatherService = mock(WeatherService.class);
    private final RateService rateService = mock(RateService.class);
    private final SettingsService service = new SettingsService(chatRepository, weatherService, rateService, PROPERTIES);

    @BeforeEach
    void chatExists() {
        when(chatRepository.findOrCreate(CHAT_ID)).thenReturn(DEFAULTS);
    }

    @Test
    void savesCanonicalCityNamesAndReportsUnknownOnes() {
        when(weatherService.resolve("brest")).thenReturn(new CityWeather.Found("Брест", "Брест"));
        when(weatherService.resolve("Минск")).thenReturn(new CityWeather.Found("Минск", "Минск"));
        when(weatherService.resolve("Абырвалг")).thenReturn(new CityWeather.NotFound("Абырвалг"));

        ListUpdate update = service.updateCities(CHAT_ID, "brest , Минск,Абырвалг");

        verify(chatRepository).updateCities(CHAT_ID, List.of("Брест", "Минск"));
        assertThat(update.message()).isEqualTo("Сохранил города: Брест, Минск\nНе нашёл: Абырвалг");
    }

    @Test
    void mergesCitiesThatResolveToSameName() {
        when(weatherService.resolve("minsk")).thenReturn(new CityWeather.Found("Минск", "Минск"));
        when(weatherService.resolve("Минск")).thenReturn(new CityWeather.Found("Минск", "Минск"));

        service.updateCities(CHAT_ID, "minsk, Минск");

        verify(chatRepository).updateCities(CHAT_ID, List.of("Минск"));
    }

    @Test
    void keepsCitiesWhenNothingWasFoundOrServiceIsDown() {
        when(weatherService.resolve("Абырвалг")).thenReturn(new CityWeather.NotFound("Абырвалг"));
        when(weatherService.resolve("Минск")).thenReturn(new CityWeather.Unavailable("Минск"));

        assertThat(service.updateCities(CHAT_ID, "Абырвалг").message()).isEqualTo("Не нашёл такие города: Абырвалг");
        assertThat(service.updateCities(CHAT_ID, "Минск").message()).isEqualTo("Сервис погоды сейчас недоступен, попробуйте позже");
        verify(chatRepository, never()).updateCities(anyLong(), anyList());
    }

    @Test
    void limitsNumberOfCities() {
        ListUpdate update = service.updateCities(CHAT_ID, "a, b, c, d");

        assertThat(update.message()).isEqualTo("Можно указать до 3 городов");
        assertThat(update.settings()).isEqualTo(DEFAULTS);
    }

    @Test
    void savesKnownCurrenciesIncludingCrypto() {
        when(rateService.quote("USD")).thenReturn(new Quote.Fiat("USD", 1, BigDecimal.ONE));
        when(rateService.quote("BTC")).thenReturn(new Quote.Crypto("BTC", BigDecimal.TEN, null));
        when(rateService.quote("XYZ")).thenReturn(new Quote.Unknown("XYZ"));

        ListUpdate update = service.updateCurrencies(CHAT_ID, "usd,btc xyz");

        verify(chatRepository).updateCurrencies(CHAT_ID, List.of("USD", "BTC"));
        assertThat(update.message()).isEqualTo("Сохранил валюты: USD, BTC\nНе нашёл: XYZ");
    }

    @Test
    void keepsCurrenciesWhenRatesAreUnavailable() {
        when(rateService.quote("USD")).thenReturn(new Quote.Unavailable("USD"));

        assertThat(service.updateCurrencies(CHAT_ID, "USD").message()).isEqualTo("Курсы сейчас недоступны, попробуйте позже");
        assertThat(service.updateCurrencies(CHAT_ID, " ").message()).startsWith("Не увидел ни одной валюты");
        verify(chatRepository, never()).updateCurrencies(anyLong(), anyList());
    }

    @Test
    void togglesDigestSections() {
        service.toggleRates(CHAT_ID);
        service.toggleWeather(CHAT_ID);

        verify(chatRepository).updateSendRates(CHAT_ID, false);
        verify(chatRepository).updateSendWeather(CHAT_ID, false);
    }
}