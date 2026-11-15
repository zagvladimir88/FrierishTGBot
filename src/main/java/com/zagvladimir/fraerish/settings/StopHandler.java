package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(31)
@Component
@RequiredArgsConstructor
public class StopHandler implements CommandHandler {

    static final String STOPPED = "Ежедневная сводка отключена. Включить снова: /s";

    private final SettingsService settingsService;
    private final SettingsAccess settingsAccess;

    @Override
    public String command() {
        return "/stop";
    }

    @Override
    public String description() {
        return "Выключить ежедневную сводку";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        if (!settingsAccess.canConfigure(request.context())) {
            return BotReply.text(SettingsAccess.ADMINS_ONLY);
        }
        settingsService.setEnabled(request.chatId(), false);
        return BotReply.text(STOPPED);
    }
}