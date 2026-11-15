package com.zagvladimir.fraerish.weather;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.command.ListInput;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Order(2)
@Component
@RequiredArgsConstructor
public class WeatherHandler implements CommandHandler {

    static final String USAGE = "Укажите город или несколько через запятую: /w Минск, Брест";

    private final WeatherService weatherService;
    private final ChatRepository chatRepository;
    private final DigestProperties digestProperties;

    @Override
    public String command() {
        return "/w";
    }

    @Override
    public String description() {
        return "Погода: /w Минск, Брест";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        List<String> cities = request.hasArgs() ? ListInput.cities(request.args()) : savedCities(request.chatId());
        if (cities.isEmpty()) {
            return BotReply.text(USAGE);
        }
        return BotReply.text(cities.stream()
                .limit(digestProperties.maxCities())
                .map(weatherService::lookup)
                .map(CityWeather::text)
                .collect(Collectors.joining("\n\n")));
    }

    private List<String> savedCities(long chatId) {
        return chatRepository.find(chatId).map(ChatSettings::cities).orElse(List.of());
    }
}