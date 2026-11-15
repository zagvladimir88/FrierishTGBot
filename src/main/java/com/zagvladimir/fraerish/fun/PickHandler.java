package com.zagvladimir.fraerish.fun;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.random.RandomGenerator;

@Order(6)
@Component
@RequiredArgsConstructor
public class PickHandler implements CommandHandler {

    static final String USAGE = "Напишите варианты через запятую: /pick бар, кино, дома";

    private final RandomGenerator random;

    @Override
    public String command() {
        return "/pick";
    }

    @Override
    public String description() {
        return "Случайный выбор: /pick бар, кино, дома";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        List<String> choices = Choices.parse(request.args());
        if (choices.size() < 2) {
            return BotReply.text(USAGE);
        }
        return BotReply.text("🎲 Выбираю: " + choices.get(random.nextInt(choices.size())));
    }
}