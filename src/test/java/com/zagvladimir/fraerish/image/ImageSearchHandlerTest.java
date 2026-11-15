package com.zagvladimir.fraerish.image;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImageSearchHandlerTest {

    private static final SearxngProperties PROPERTIES = new SearxngProperties("http://searxng.test", 3, 1);

    @Test
    void repliesWithFoundImagesAndPassesConfiguredLimit() {
        List<String> calls = new ArrayList<>();
        ImageSearchClient client = (query, limit) -> {
            calls.add(query + "|" + limit);
            return List.of("https://img.test/1.jpg", "https://img.test/2.jpg");
        };

        BotReply reply = new ImageSearchHandler(client, PROPERTIES).handle(new CommandRequest(1, "рыжие котики"));

        assertThat(reply).isEqualTo(BotReply.photos(List.of("https://img.test/1.jpg", "https://img.test/2.jpg")));
        assertThat(calls).containsExactly("рыжие котики|3");
    }

    @Test
    void reportsWhenNothingFound() {
        ImageSearchHandler handler = new ImageSearchHandler((query, limit) -> List.of(), PROPERTIES);

        assertThat(handler.handle(new CommandRequest(1, "абырвалг"))).isEqualTo(BotReply.text(ImageSearchHandler.NOTHING_FOUND));
    }

    @Test
    void rejectsEmptyQueryWithoutSearching() {
        ImageSearchHandler handler = new ImageSearchHandler((query, limit) -> {
            throw new AssertionError("search must not be called");
        }, PROPERTIES);

        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(ImageSearchHandler.USAGE));
    }
}