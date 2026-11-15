package com.zagvladimir.fraerish.story;

import java.util.List;

public record StoryGame(long id, long chatId, int messageId, String pollId, List<String> options, Integer answerIndex) {

    public StoryGame {
        options = List.copyOf(options);
    }

    public boolean open() {
        return answerIndex == null;
    }
}