package com.zagvladimir.fraerish.command;

import java.util.List;

public sealed interface BotReply {

    static BotReply text(String text) {
        return new Text(text);
    }

    static BotReply photos(List<String> urls) {
        return new Photos(List.copyOf(urls));
    }

    record Text(String text) implements BotReply {
    }

    record Photos(List<String> urls) implements BotReply {
    }
}