package com.zagvladimir.fraerish.command;

import java.util.Optional;

public record CommandRequest(ChatContext context, String args, String repliedText) {

    public CommandRequest(ChatContext context, String args) {
        this(context, args, null);
    }

    public CommandRequest(long chatId, String args) {
        this(ChatContext.privateChat(chatId), args);
    }

    public long chatId() {
        return context.chatId();
    }

    public boolean hasArgs() {
        return !args.isBlank();
    }

    public Optional<String> replied() {
        return Optional.ofNullable(repliedText).filter(text -> !text.isBlank());
    }
}