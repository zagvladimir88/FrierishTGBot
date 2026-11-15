package com.zagvladimir.fraerish.command;

import java.util.List;

public sealed interface BotReply {

    static BotReply text(String text) {
        return new Text(text);
    }

    static BotReply photos(List<Photo> photos) {
        return new Photos(List.copyOf(photos));
    }

    record Text(String text) implements BotReply {
    }

    record Photos(List<Photo> photos) implements BotReply {
    }

    record Photo(String fileName, byte[] content) {
    }
}