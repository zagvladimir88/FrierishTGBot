package com.zagvladimir.fraerish.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAdminChecker {

    private static final Set<String> ADMIN_STATUSES = Set.of("creator", "administrator");

    private final TelegramClient telegramClient;

    public boolean isAdmin(long chatId, long userId) {
        try {
            String status = telegramClient.execute(GetChatMember.builder().chatId(chatId).userId(userId).build()).getStatus();
            return ADMIN_STATUSES.contains(status);
        } catch (TelegramApiException e) {
            log.warn("Could not check admin rights of user {} in chat {}: {}", userId, chatId, e.getMessage());
            return false;
        }
    }
}