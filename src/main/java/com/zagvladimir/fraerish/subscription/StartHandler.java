package com.zagvladimir.fraerish.subscription;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartHandler implements CommandHandler {

    static final String HELP = """
            Добро пожаловать в Fraerish бот!

            Доступные команды:
            /w - прогноз погоды "/w Лондон"
            /exchange - Курс обмена валют BYN "/exchange USD"
            /g - Поиск изображений "/g котики"
            /c - Исправление неправильной раскладки "/c ghbdtn"
            /stop - Отключить ежедневную рассылку курса и погоды""";

    private final ChatRepository chatRepository;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        chatRepository.activate(request.chatId());
        return BotReply.text(HELP);
    }
}