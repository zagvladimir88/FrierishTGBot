package com.zagvladimir.fraerish.image;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageSearchHandlerTest {

    private static final SearxngProperties PROPERTIES = new SearxngProperties("http://searxng.test", 10, 3, 1);
    private static final List<ImageCandidate> CANDIDATES = List.of(new ImageCandidate("https://img.test/1.jpg", null));

    private final PhotoFetcher photoFetcher = mock(PhotoFetcher.class);

    @Test
    void searchesConfiguredNumberOfCandidatesAndRepliesWithDownloadedPhotos() {
        List<String> searches = new ArrayList<>();
        ImageSearchClient client = (query, limit) -> {
            searches.add(query + "|" + limit);
            return CANDIDATES;
        };
        BotReply.Photo photo = new BotReply.Photo("image-1.jpg", new byte[]{1});
        when(photoFetcher.fetch(CANDIDATES, 3)).thenReturn(List.of(photo));

        BotReply reply = new ImageSearchHandler(client, photoFetcher, PROPERTIES).handle(new CommandRequest(1, "рыжие котики"));

        assertThat(reply).isEqualTo(BotReply.photos(List.of(photo)));
        assertThat(searches).containsExactly("рыжие котики|10");
    }

    @Test
    void reportsWhenNoPhotoCouldBeDownloaded() {
        when(photoFetcher.fetch(anyList(), anyInt())).thenReturn(List.of());
        ImageSearchHandler handler = new ImageSearchHandler((query, limit) -> CANDIDATES, photoFetcher, PROPERTIES);

        assertThat(handler.handle(new CommandRequest(1, "абырвалг"))).isEqualTo(BotReply.text(ImageSearchHandler.NOTHING_FOUND));
    }

    @Test
    void rejectsEmptyQueryWithoutSearching() {
        ImageSearchHandler handler = new ImageSearchHandler((query, limit) -> {
            throw new AssertionError("search must not be called");
        }, photoFetcher, PROPERTIES);

        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(ImageSearchHandler.USAGE));
        verifyNoInteractions(photoFetcher);
    }
}