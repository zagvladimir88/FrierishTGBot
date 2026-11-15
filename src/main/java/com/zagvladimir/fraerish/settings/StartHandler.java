package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.digest.ChatSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
@RequiredArgsConstructor
public class StartHandler implements CommandHandler {

    static final String HELP = """
            Добро пожаловать в Fraerish бот!

            Команды:
            /w Минск, Брест — погода сейчас и на ближайшие часы
            /e USD, EUR, BTC — курсы НБРБ и криптовалют
            /e 100 usd eur — пересчитать сумму
            /g котики — поиск картинок
            /c ghbdtn — исправление неправильной раскладки (или ответом на сообщение)
            /pick бар, кино, дома — случайный выбор
            /poll Куда идём? Бар, Кино — опрос
            /top — кто больше всех писал в группе за неделю
            /s — настройки ежедневной сводки
            /stop — отключить сводку

            /w и /e без параметров покажут города и валюты из настроек сводки.
            Ссылки на TikTok, Reels и Shorts бот сам превращает в видео.""";

    private final SettingsService settingsService;
    private final SettingsMenu settingsMenu;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String description() {
        return "Справка и меню ежедневной сводки";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        ChatSettings settings = request.context().groupChat()
                ? settingsService.settings(request.chatId())
                : settingsService.setEnabled(request.chatId(), true);
        return settingsMenu.main(settings, HELP);
    }
}