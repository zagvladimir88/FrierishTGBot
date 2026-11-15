package com.zagvladimir.fraerish.fun;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(7)
@Component
public class PollHandler implements CommandHandler {

    static final String USAGE = "Напишите вопрос и варианты через запятую: /poll Куда идём? Бар, Кино, Дома";
    static final int MAX_QUESTION_LENGTH = 300;
    static final int MAX_OPTIONS = 12;

    @Override
    public String command() {
        return "/poll";
    }

    @Override
    public String description() {
        return "Опрос: /poll Куда идём? Бар, Кино, Дома";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        String text = request.args().strip();
        int questionEnd = text.indexOf('?');
        String question;
        List<String> options;
        if (questionEnd >= 0) {
            question = text.substring(0, questionEnd + 1).strip();
            options = Choices.parse(text.substring(questionEnd + 1));
        } else {
            List<String> parts = Choices.parse(text);
            question = parts.isEmpty() ? "" : parts.getFirst();
            options = parts.isEmpty() ? List.of() : parts.subList(1, parts.size());
        }
        if (question.isEmpty() || question.length() > MAX_QUESTION_LENGTH || options.size() < 2) {
            return BotReply.text(USAGE);
        }
        if (options.size() > MAX_OPTIONS) {
            return BotReply.text("В опросе может быть не больше %d вариантов".formatted(MAX_OPTIONS));
        }
        return new BotReply.Poll(question, options);
    }
}