package com.zagvladimir.fraerish.digest;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.exchange.Quote;
import com.zagvladimir.fraerish.exchange.RateService;
import com.zagvladimir.fraerish.telegram.BotSender;
import com.zagvladimir.fraerish.telegram.SendResult;
import com.zagvladimir.fraerish.weather.CityWeather;
import com.zagvladimir.fraerish.weather.WeatherService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.zagvladimir.fraerish.digest.DigestTestData.PROPERTIES;
import static com.zagvladimir.fraerish.digest.DigestTestData.chat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DigestSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final LocalTime ELEVEN = LocalTime.of(11, 0);
    private static final ChatSettings USD_MINSK = chat(1, ELEVEN, List.of("Минск"), List.of("USD"));

    private final ChatRepository chatRepository = mock(ChatRepository.class);
    private final RateService rateService = mock(RateService.class);
    private final WeatherService weatherService = mock(WeatherService.class);
    private final BotSender botSender = mock(BotSender.class);

    @Test
    void sendsDueDigestAndRemembersTheDay() {
        DigestScheduler scheduler = schedulerAt("2026-11-16T08:05:00Z");
        when(chatRepository.findDue(TODAY, LocalTime.of(11, 5))).thenReturn(List.of(USD_MINSK));
        sourcesAreHealthy();
        when(botSender.send(eq(1L), any())).thenReturn(SendResult.SENT);

        scheduler.sendDueDigests();

        verify(botSender).send(eq(1L), any(BotReply.Text.class));
        verify(chatRepository).markDigestSent(1L, TODAY);
    }

    @Test
    void asksEachSourceOnceForChatsWithSameSettings() {
        DigestScheduler scheduler = schedulerAt("2026-11-16T08:05:00Z");
        when(chatRepository.findDue(any(), any())).thenReturn(List.of(USD_MINSK, chat(2, ELEVEN, List.of("минск"), List.of("USD"))));
        sourcesAreHealthy();
        when(botSender.send(anyLong(), any())).thenReturn(SendResult.SENT);

        scheduler.sendDueDigests();

        verify(rateService, times(1)).quote("USD");
        verify(weatherService, times(1)).lookup(anyString());
        verify(botSender, times(2)).send(anyLong(), any());
    }

    @Test
    void waitsForUnavailableSourcesWithinRetryWindow() {
        DigestScheduler scheduler = schedulerAt("2026-11-16T08:10:00Z");
        when(chatRepository.findDue(any(), any())).thenReturn(List.of(USD_MINSK));
        when(rateService.quote("USD")).thenReturn(new Quote.Unavailable("USD"));
        when(weatherService.lookup(anyString())).thenReturn(new CityWeather.Found("Минск", "Минск ясно"));

        scheduler.sendDueDigests();

        verifyNoInteractions(botSender);
        verify(chatRepository, never()).markDigestSent(anyLong(), any());
    }

    @Test
    void sendsWhatIsAvailableAfterRetryWindow() {
        DigestScheduler scheduler = schedulerAt("2026-11-16T08:31:00Z");
        when(chatRepository.findDue(any(), any())).thenReturn(List.of(USD_MINSK));
        when(rateService.quote("USD")).thenReturn(new Quote.Unavailable("USD"));
        when(weatherService.lookup(anyString())).thenReturn(new CityWeather.Found("Минск", "Минск ясно"));
        when(botSender.send(anyLong(), any())).thenReturn(SendResult.SENT);

        scheduler.sendDueDigests();

        verify(botSender).send(eq(1L), any());
        verify(chatRepository).markDigestSent(1L, TODAY);
    }

    @Test
    void disablesChatsThatBlockedTheBot() {
        DigestScheduler scheduler = schedulerAt("2026-11-16T08:05:00Z");
        when(chatRepository.findDue(any(), any())).thenReturn(List.of(USD_MINSK));
        sourcesAreHealthy();
        when(botSender.send(anyLong(), any())).thenReturn(SendResult.BLOCKED);

        scheduler.sendDueDigests();

        verify(chatRepository).setEnabled(1L, false);
        verify(chatRepository, never()).markDigestSent(anyLong(), any());
    }

    @Test
    void retriesFailedDeliveryOnlyWithinWindow() {
        when(chatRepository.findDue(any(), any())).thenReturn(List.of(USD_MINSK));
        sourcesAreHealthy();
        when(botSender.send(anyLong(), any())).thenReturn(SendResult.FAILED);

        schedulerAt("2026-11-16T08:05:00Z").sendDueDigests();
        verify(chatRepository, never()).markDigestSent(anyLong(), any());

        schedulerAt("2026-11-16T08:45:00Z").sendDueDigests();
        verify(chatRepository).markDigestSent(1L, TODAY);
    }

    @Test
    void skipsChatsWithNothingToSend() {
        DigestScheduler scheduler = schedulerAt("2026-11-16T08:05:00Z");
        when(chatRepository.findDue(any(), any())).thenReturn(List.of(chat(1, ELEVEN, List.of("Минск"), List.of("USD"), false, false, null)));

        scheduler.sendDueDigests();

        verifyNoInteractions(rateService, weatherService, botSender);
    }

    @Test
    void usesMinskDateAroundMidnight() {
        DigestScheduler scheduler = schedulerAt("2026-11-15T21:30:00Z");
        when(chatRepository.findDue(any(), any())).thenReturn(List.of());

        scheduler.sendDueDigests();

        verify(chatRepository).findDue(TODAY, LocalTime.of(0, 30));
        assertThat(PROPERTIES.zone().getId()).isEqualTo("Europe/Minsk");
    }

    private void sourcesAreHealthy() {
        when(rateService.quote("USD")).thenReturn(new Quote.Fiat("USD", 1, new BigDecimal("3.0026")));
        when(weatherService.lookup(anyString())).thenReturn(new CityWeather.Found("Минск", "Минск ясно"));
    }

    private DigestScheduler schedulerAt(String instant) {
        return new DigestScheduler(chatRepository, rateService, weatherService, botSender, PROPERTIES,
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }
}