package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsHandler implements CommandHandler {

    private final SettingsService settingsService;
    private final SettingsMenu settingsMenu;

    @Override
    public String command() {
        return "/settings";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        return settingsMenu.main(settingsService.settings(request.chatId()));
    }
}