package com.zagvladimir.fraerish.command;

public record CommandRequest(ChatContext context, String args) {

    public CommandRequest(long chatId, String args) {
        this(ChatContext.privateChat(chatId), args);
    }

    public long chatId() {
        return context.chatId();
    }

    public boolean hasArgs() {
        return !args.isBlank();
    }
}