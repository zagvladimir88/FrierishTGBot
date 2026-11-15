package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.stats.ChatStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class MessageCounter implements MessageObserver {

    private final ChatStats chatStats;

    @Override
    public void observe(Message message) {
        User author = message.getFrom();
        if (author == null || Boolean.TRUE.equals(author.getIsBot()) || message.getSenderChat() != null) {
            return;
        }
        chatStats.countMessage(message.getChatId(), author.getId(), Users.displayName(author));
    }
}