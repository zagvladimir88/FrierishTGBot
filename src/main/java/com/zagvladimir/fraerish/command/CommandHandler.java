package com.zagvladimir.fraerish.command;

import java.util.List;

public interface CommandHandler {

    String command();

    String description();

    BotReply handle(CommandRequest request);

    default List<String> aliases() {
        return List.of();
    }

    default boolean acceptsEditedMessages() {
        return false;
    }
}