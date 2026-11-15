package com.zagvladimir.fraerish.video;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.telegram.BotSender;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VideoLinkListenerTest {

    private static final String SHORTS = "https://www.youtube.com/shorts/ojsibtSeGfw";

    private final VideoDownloader downloader = mock(VideoDownloader.class);
    private final BotSender sender = mock(BotSender.class);

    @Test
    void repliesToMessageWithDownloadedVideo() {
        BotReply.Video video = new BotReply.Video("video.mp4", new byte[]{1}, 1080, 1920, 15);
        when(downloader.download(SHORTS)).thenReturn(Optional.of(video));

        boolean handled = listener(true).onText(42L, 7, "глянь " + SHORTS);

        assertThat(handled).isTrue();
        verify(sender).showUploadingVideo(42L);
        verify(sender).send(42L, video, 7);
    }

    @Test
    void tellsWhenVideoCouldNotBeDownloaded() {
        when(downloader.download(SHORTS)).thenReturn(Optional.empty());

        listener(true).onText(42L, 7, SHORTS);

        verify(sender).send(42L, BotReply.text(VideoLinkListener.DOWNLOAD_FAILED), 7);
    }

    @Test
    void ignoresTextWithoutSupportedLinksAndWorksOnlyWhenEnabled() {
        assertThat(listener(true).onText(42L, 7, "привет")).isFalse();
        assertThat(listener(false).onText(42L, 7, SHORTS)).isFalse();
        verifyNoInteractions(downloader, sender);
    }

    private VideoLinkListener listener(boolean enabled) {
        VideoProperties properties = new VideoProperties(enabled, "yt-dlp", Duration.ofSeconds(30), DataSize.ofMegabytes(50), 720, 2);
        return new VideoLinkListener(downloader, sender, properties, new DirectExecutor());
    }

    private static final class DirectExecutor extends AbstractExecutorService {

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }
}