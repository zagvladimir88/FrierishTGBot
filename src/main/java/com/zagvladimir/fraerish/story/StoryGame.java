package com.zagvladimir.fraerish.story;

import java.util.List;

public record StoryGame(long id, long chatId, int messageId, String pollId, List<String> options, Integer answerIndex,
                        Integer confirmMessageId, Integer votersSnapshot) {

    public StoryGame {
        options = List.copyOf(options);
    }

    public StoryGame(long id, long chatId, int messageId, String pollId, List<String> options, Integer answerIndex) {
        this(id, chatId, messageId, pollId, options, answerIndex, null, null);
    }

    public boolean open() {
        return answerIndex == null;
    }

    public boolean confirming() {
        return confirmMessageId != null;
    }

    public int majority() {
        return votersSnapshot == null ? 1 : votersSnapshot / 2 + 1;
    }
}