package com.zagvladimir.fraerish.weather;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class WeatherHandler implements CommandHandler {

    static final String USAGE = "Укажите город: /w Минск";
    static final String CITY_NOT_FOUND = "Incorrect city name";

    private static final Map<String, String> EMOJI_BY_CONDITION = Map.of(
            "clear", "☀️",
            "clouds", "☁️",
            "rain", "🌧️",
            "snow", "❄️");

    private final OpenWeatherClient openWeatherClient;

    @Override
    public String command() {
        return "/w";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        return request.hasArgs() ? forecast(request.args()) : BotReply.text(USAGE);
    }

    public BotReply forecast(String city) {
        try {
            return BotReply.text(format(openWeatherClient.currentWeather(city)));
        } catch (HttpClientErrorException.NotFound e) {
            return BotReply.text(CITY_NOT_FOUND);
        }
    }

    private static String format(CurrentWeather weather) {
        CurrentWeather.Condition condition = weather.condition();
        String emoji = EMOJI_BY_CONDITION.getOrDefault(condition.main().toLowerCase(Locale.ROOT), "");
        String summary = Stream.of(weather.name(), condition.description(), emoji)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return "%s\nТемпература %d°C, ощущается как %d°C".formatted(
                summary,
                Math.round(weather.main().temp()),
                Math.round(weather.main().feelsLike()));
    }
}