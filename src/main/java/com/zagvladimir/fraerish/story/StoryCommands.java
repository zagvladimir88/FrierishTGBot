package com.zagvladimir.fraerish.story;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.command.ListInput;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Pattern;

@Configuration
@RequiredArgsConstructor
public class StoryCommands {

    private static final Pattern OPTION_SEPARATOR = Pattern.compile("\\s*[,;\\n]\\s*");

    private final StoryService storyService;

    @Bean
    @Order(20)
    public CommandHandler storyHandler() {
        return handler("/story", "Угадайка: что в новой сторис", request -> storyService.start(request.chatId()));
    }

    @Bean
    @Order(21)
    public CommandHandler storyAnswerHandler() {
        return handler("/answer", "Закрыть угадайку вручную (админы): /answer качалка", request -> storyService.answer(request.context(), request.args()));
    }

    @Bean
    @Order(22)
    public CommandHandler storyTopHandler() {
        return handler("/story_top", "Кто лучше угадывает сторис", request -> storyService.top(request.chatId()));
    }

    @Bean
    @Order(23)
    public CommandHandler storyOptionsHandler() {
        return handler("/story_options", "Варианты угадайки: /story_options Пейзаж, Селфи",
                request -> storyService.changeOptions(request.context(), ListInput.distinctIgnoringCase(Arrays.stream(
                        OPTION_SEPARATOR.split(request.args().strip())).filter(option -> !option.isBlank()).toList())));
    }

    @Bean
    @Order(24)
    public CommandHandler storyPlayersHandler() {
        return handler("/story_players", "Сколько игроков ждать в угадайке: /story_players 3 или авто",
                request -> storyService.changePlayers(request.context(), request.args()));
    }

    private static CommandHandler handler(String command, String description, Function<CommandRequest, BotReply> action) {
        return new CommandHandler() {
            @Override
            public String command() {
                return command;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public BotReply handle(CommandRequest request) {
                return action.apply(request);
            }
        };
    }
}