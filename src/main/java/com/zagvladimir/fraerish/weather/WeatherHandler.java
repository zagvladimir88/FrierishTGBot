package com.zagvladimir.fraerish.weather;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.command.ListInput;
import com.zagvladimir.fraerish.digest.DigestProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WeatherHandler implements CommandHandler {

    static final String USAGE = "Укажите город или несколько через запятую: /w Минск, Брест";

    private final WeatherService weatherService;
    private final DigestProperties digestProperties;

    @Override
    public String command() {
        return "/w";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        List<String> cities = ListInput.cities(request.args());
        if (cities.isEmpty()) {
            return BotReply.text(USAGE);
        }
        return BotReply.text(cities.stream()
                .limit(digestProperties.maxCities())
                .map(weatherService::lookup)
                .map(CityWeather::text)
                .collect(Collectors.joining("\n\n")));
    }
}