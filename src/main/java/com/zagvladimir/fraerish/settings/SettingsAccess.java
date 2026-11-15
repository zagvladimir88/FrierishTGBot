package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.telegram.ChatAdminChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsAccess {

    static final String ADMINS_ONLY = "Настройки сводки меняют только админы группы";

    private final ChatAdminChecker chatAdminChecker;

    public boolean canConfigure(ChatContext context) {
        return !context.groupChat()
                || context.anonymousAdmin()
                || chatAdminChecker.isAdmin(context.chatId(), context.userId());
    }
}