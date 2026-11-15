package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(6)
@Component
@RequiredArgsConstructor
public class SettingsHandler implements CommandHandler {

    private final SettingsService settingsService;
    private final SettingsMenu settingsMenu;

    @Override
    public String command() {
        return "/s";
    }

    @Override
    public List<String> aliases() {
        return List.of("/settings");
    }

    @Override
    public String description() {
        return "Настройки ежедневной сводки";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        return settingsMenu.main(settingsService.settings(request.chatId()));
    }
}