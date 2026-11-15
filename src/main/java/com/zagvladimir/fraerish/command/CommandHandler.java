package com.zagvladimir.fraerish.command;

public interface CommandHandler {

    String command();

    BotReply handle(CommandRequest request);

    default boolean acceptsEditedMessages() {
        return false;
    }
}