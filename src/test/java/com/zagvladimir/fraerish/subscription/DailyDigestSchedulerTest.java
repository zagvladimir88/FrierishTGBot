package com.zagvladimir.fraerish.subscription;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.exchange.ExchangeHandler;
import com.zagvladimir.fraerish.telegram.BotProperties;
import com.zagvladimir.fraerish.telegram.BotSender;
import com.zagvladimir.fraerish.telegram.SendResult;
import com.zagvladimir.fraerish.weather.WeatherHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DailyDigestSchedulerTest {

    private static final BotReply RATE = BotReply.text("Курс 1 Доллар США - 3.0026 BYN");
    private static final BotReply WEATHER = BotReply.text("Минск ясно");

    private final ChatRepository chatRepository = mock(ChatRepository.class);
    private final ExchangeHandler exchangeHandler = mock(ExchangeHandler.class);
    private final WeatherHandler weatherHandler = mock(WeatherHandler.class);
    private final BotSender botSender = mock(BotSender.class);
    private final DailyDigestScheduler scheduler = new DailyDigestScheduler(chatRepository, exchangeHandler, weatherHandler,
            botSender, new BotProperties("token", "FraerishBot", new BotProperties.Schedule("USD", "Минск")));

    @Test
    void sendsConfiguredCurrencyRateToEveryActiveChat() {
        when(chatRepository.findActiveChatIds()).thenReturn(List.of(1L, 2L));
        when(exchangeHandler.rate("USD")).thenReturn(RATE);
        when(botSender.send(anyLong(), any())).thenReturn(SendResult.SENT);

        scheduler.sendExchangeRate();

        verify(botSender).send(1L, RATE);
        verify(botSender).send(2L, RATE);
        verify(chatRepository, never()).deactivate(anyLong());
    }

    @Test
    void deactivatesChatsThatBlockedTheBot() {
        when(chatRepository.findActiveChatIds()).thenReturn(List.of(1L, 2L));
        when(weatherHandler.forecast("Минск")).thenReturn(WEATHER);
        when(botSender.send(1L, WEATHER)).thenReturn(SendResult.BLOCKED);
        when(botSender.send(2L, WEATHER)).thenReturn(SendResult.FAILED);

        scheduler.sendWeather();

        verify(chatRepository).deactivate(1L);
        verify(chatRepository, never()).deactivate(2L);
    }

    @Test
    void doesNotCallExternalServiceWhenNobodySubscribed() {
        when(chatRepository.findActiveChatIds()).thenReturn(List.of());

        scheduler.sendExchangeRate();

        verifyNoInteractions(exchangeHandler, botSender);
    }

    @Test
    void skipsDigestWhenExternalServiceFails() {
        when(chatRepository.findActiveChatIds()).thenReturn(List.of(1L));
        when(weatherHandler.forecast(anyString())).thenThrow(new IllegalStateException("OpenWeather is down"));

        scheduler.sendWeather();

        verifyNoInteractions(botSender);
    }
}