package com.zagvladimir.fraerish.alerts;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import com.zagvladimir.fraerish.telegram.BotSender;
import com.zagvladimir.fraerish.telegram.SendResult;
import com.zagvladimir.fraerish.weather.PrecipitationAlert;
import com.zagvladimir.fraerish.weather.WeatherService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WeatherAlertSchedulerTest {

    private static final WeatherAlertProperties PROPERTIES = new WeatherAlertProperties(
            LocalTime.of(7, 0), LocalTime.of(22, 0), Duration.ofHours(3), 0.5);
    private static final DigestProperties DIGEST = new DigestProperties(ZoneId.of("Europe/Minsk"), "по Минску", Duration.ofMinutes(30), 5, 5);
    private static final PrecipitationAlert MINSK_RAIN = new PrecipitationAlert("Минск", "🌧️ Минск: с 12:00 возможен дождь (70%)");

    private final ChatRepository chatRepository = mock(ChatRepository.class);
    private final WeatherService weatherService = mock(WeatherService.class);
    private final BotSender botSender = mock(BotSender.class);

    @Test
    void warnsEachChatOncePerDayAndAsksForecastOncePerCity() {
        when(chatRepository.findWeatherAlertSubscribers()).thenReturn(List.of(chat(1, "Минск", "Брест"), chat(2, "минск")));
        when(weatherService.precipitationAlert(eq("Минск"), any(), any(), anyDouble())).thenReturn(Optional.of(MINSK_RAIN));
        when(weatherService.precipitationAlert(eq("Брест"), any(), any(), anyDouble())).thenReturn(Optional.empty());
        when(botSender.send(anyLong(), any())).thenReturn(SendResult.SENT);
        WeatherAlertScheduler scheduler = schedulerAt("2026-11-16T06:05:00Z");

        scheduler.checkWeather();
        scheduler.checkWeather();

        verify(botSender, times(1)).send(1L, BotReply.text(MINSK_RAIN.text()));
        verify(botSender, times(1)).send(2L, BotReply.text(MINSK_RAIN.text()));
        verify(weatherService, times(1)).precipitationAlert(eq("Минск"), any(), any(), anyDouble());
    }

    @Test
    void keepsCheckingCityWithoutAlertLaterTheSameDay() {
        when(chatRepository.findWeatherAlertSubscribers()).thenReturn(List.of(chat(1, "Минск")));
        when(weatherService.precipitationAlert(anyString(), any(), any(), anyDouble()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(MINSK_RAIN));
        when(botSender.send(anyLong(), any())).thenReturn(SendResult.SENT);
        WeatherAlertScheduler scheduler = schedulerAt("2026-11-16T06:05:00Z");

        scheduler.checkWeather();
        scheduler.checkWeather();

        verify(botSender, times(1)).send(1L, BotReply.text(MINSK_RAIN.text()));
    }

    @Test
    void staysQuietAtNight() {
        schedulerAt("2026-11-16T20:05:00Z").checkWeather();

        verifyNoInteractions(chatRepository, weatherService, botSender);
    }

    @Test
    void retriesFailedDeliveryAndDisablesBlockedChats() {
        when(chatRepository.findWeatherAlertSubscribers()).thenReturn(List.of(chat(1, "Минск"), chat(2, "Минск")));
        when(weatherService.precipitationAlert(anyString(), any(), any(), anyDouble())).thenReturn(Optional.of(MINSK_RAIN));
        when(botSender.send(eq(1L), any())).thenReturn(SendResult.FAILED);
        when(botSender.send(eq(2L), any())).thenReturn(SendResult.BLOCKED);
        WeatherAlertScheduler scheduler = schedulerAt("2026-11-16T06:05:00Z");

        scheduler.checkWeather();
        scheduler.checkWeather();

        verify(botSender, times(2)).send(eq(1L), any());
        verify(chatRepository, times(2)).setEnabled(2L, false);
        verify(chatRepository, never()).setEnabled(1L, false);
    }

    private static ChatSettings chat(long chatId, String... cities) {
        return new ChatSettings(chatId, true, LocalTime.of(11, 0), List.of(cities), List.of("USD"), true, true, null, true);
    }

    private WeatherAlertScheduler schedulerAt(String instant) {
        return new WeatherAlertScheduler(chatRepository, weatherService, botSender, PROPERTIES, DIGEST,
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }
}