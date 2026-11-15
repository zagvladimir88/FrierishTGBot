package com.zagvladimir.fraerish.weather;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WeatherHandlerTest {

    private static final DigestProperties PROPERTIES = new DigestProperties(ZoneId.of("Europe/Minsk"), "по Минску", Duration.ofMinutes(30), 2, 5);

    private final WeatherService weatherService = mock(WeatherService.class);
    private final ChatRepository chatRepository = mock(ChatRepository.class);
    private final WeatherHandler handler = new WeatherHandler(weatherService, chatRepository, PROPERTIES);

    @Test
    void showsWeatherForEveryRequestedCityUpToLimit() {
        when(weatherService.lookup(anyString())).thenAnswer(invocation -> new CityWeather.Found(invocation.getArgument(0), invocation.getArgument(0) + " ясно"));

        BotReply reply = handler.handle(new CommandRequest(1, "минск, Брест, Гомель"));

        assertThat(reply).isEqualTo(BotReply.text("минск ясно\n\nБрест ясно"));
        verify(weatherService).lookup("минск");
        verify(weatherService).lookup("Брест");
    }

    @Test
    void usesChatCitiesWhenCommandHasNoArguments() {
        when(chatRepository.find(1)).thenReturn(Optional.of(
                new ChatSettings(1, true, LocalTime.of(11, 0), List.of("Минск"), List.of("USD"), true, true, null)));
        when(weatherService.lookup("Минск")).thenReturn(new CityWeather.Found("Минск", "Минск ясно"));

        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text("Минск ясно"));
    }

    @Test
    void explainsUsageWithoutCities() {
        when(chatRepository.find(1)).thenReturn(Optional.empty());

        assertThat(handler.handle(new CommandRequest(1, " , "))).isEqualTo(BotReply.text(WeatherHandler.USAGE));
        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(WeatherHandler.USAGE));
        verifyNoInteractions(weatherService);
    }
}