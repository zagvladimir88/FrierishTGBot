package com.zagvladimir.fraerish.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMembers implements MessageObserver {

    private static final int THIS_BOT = 1;

    private final TelegramClient telegramClient;
    private final KnownBotsRepository knownBots;
    private final BotProperties properties;

    public Optional<Integer> people(long chatId) {
        try {
            int members = telegramClient.execute(GetChatMemberCount.builder().chatId(chatId).build());
            return Optional.of(Math.max(0, members - THIS_BOT - knownBots.count(chatId)));
        } catch (TelegramApiException e) {
            log.warn("Could not count members of chat {}: {}", chatId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void observe(Message message) {
        long chatId = message.getChatId();
        try {
            User author = message.getSenderChat() == null ? message.getFrom() : null;
            Stream.concat(Stream.ofNullable(author), Stream.ofNullable(message.getNewChatMembers()).flatMap(List::stream))
                    .filter(this::isOtherBot)
                    .forEach(bot -> knownBots.remember(chatId, bot.getId()));
            User left = message.getLeftChatMember();
            if (left != null && isOtherBot(left)) {
                knownBots.forget(chatId, left.getId());
            }
        } catch (DataAccessException e) {
            log.warn("Could not remember bots of chat {}: {}", chatId, e.getMessage());
        }
    }

    private boolean isOtherBot(User user) {
        return Boolean.TRUE.equals(user.getIsBot()) && !properties.name().equalsIgnoreCase(user.getUserName());
    }
}