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

    record Menu(String text, List<List<Button>> keyboard) implements BotReply {

        public Menu {
            keyboard = keyboard.stream().map(List::copyOf).toList();
        }
    }

    record Prompt(String text) implements BotReply {
    }

    record Photo(String fileName, byte[] content) {
    }

    record Button(String text, String data) {
    }
}