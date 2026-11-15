package com.zagvladimir.fraerish.command;

public record ChatContext(long chatId, long userId, boolean groupChat, boolean anonymousAdmin) {

    public static ChatContext privateChat(long chatId) {
        return new ChatContext(chatId, chatId, false, false);
    }
}