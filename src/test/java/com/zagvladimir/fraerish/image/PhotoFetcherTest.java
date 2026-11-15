package com.zagvladimir.fraerish.image;

import com.zagvladimir.fraerish.command.BotReply;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhotoFetcherTest {

    private static final DownloadedImage FIRST = new DownloadedImage(ImageFormat.JPEG, new byte[]{1});
    private static final DownloadedImage SECOND = new DownloadedImage(ImageFormat.PNG, new byte[]{2});
    private static final DownloadedImage THIRD = new DownloadedImage(ImageFormat.WEBP, new byte[]{3});
    private static final DownloadedImage FOURTH = new DownloadedImage(ImageFormat.JPEG, new byte[]{4});

    private final ImageDownloader downloader = mock(ImageDownloader.class);
    private final PhotoFetcher fetcher = new PhotoFetcher(downloader, properties(Duration.ofSeconds(5)));

    @Test
    void keepsSearchOrderAndStopsAtLimit() {
        serve(Map.of("https://a", FIRST, "https://b", SECOND, "https://c", THIRD, "https://d", FOURTH));

        List<BotReply.Photo> photos = fetcher.fetch(candidates("https://a", "https://b", "https://c", "https://d"), 3);

        assertThat(photos).extracting(BotReply.Photo::fileName).containsExactly("image-1.jpg", "image-2.png", "image-3.webp");
        assertThat(photos).extracting(BotReply.Photo::content).containsExactly(FIRST.content(), SECOND.content(), THIRD.content());
    }

    @Test
    void fallsBackToThumbnailAndSkipsUnavailableImages() {
        serve(Map.of("https://b-thumb", SECOND, "https://c", THIRD));

        List<BotReply.Photo> photos = fetcher.fetch(List.of(
                new ImageCandidate("https://a", null),
                new ImageCandidate("https://b", "https://b-thumb"),
                new ImageCandidate("https://c", null)), 3);

        assertThat(photos).extracting(BotReply.Photo::fileName).containsExactly("image-1.png", "image-2.webp");
    }

    @Test
    void skipsDownloadsThatDoNotFinishInTime() {
        PhotoFetcher impatientFetcher = new PhotoFetcher(downloader, properties(Duration.ofMillis(300)));
        when(downloader.download(anyString())).thenAnswer(invocation -> {
            if (invocation.getArgument(0).equals("https://slow")) {
                Thread.sleep(Duration.ofSeconds(10));
            }
            return Optional.of(FIRST);
        });
        long started = System.nanoTime();

        List<BotReply.Photo> photos = impatientFetcher.fetch(candidates("https://slow", "https://fast"), 3);

        assertThat(photos).extracting(BotReply.Photo::fileName).containsExactly("image-1.jpg");
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void returnsNothingWhenThereAreNoCandidates() {
        assertThat(fetcher.fetch(List.of(), 3)).isEmpty();
    }

    private void serve(Map<String, DownloadedImage> images) {
        when(downloader.download(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(images.get(invocation.<String>getArgument(0))));
    }

    private static List<ImageCandidate> candidates(String... urls) {
        return Arrays.stream(urls).map(url -> new ImageCandidate(url, null)).toList();
    }

    private static ImageDownloadProperties properties(Duration totalTimeout) {
        return new ImageDownloadProperties(Duration.ofSeconds(1), Duration.ofSeconds(1), totalTimeout, DataSize.ofMegabytes(10));
    }
}