package com.zagvladimir.fraerish.video;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.telegram.BotSender;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
public class VideoLinkListener {

    static final String DOWNLOAD_FAILED = "Не получилось скачать видео 😕";

    private final VideoDownloader videoDownloader;
    private final BotSender botSender;
    private final VideoProperties properties;
    private final ExecutorService executor;
    private final Semaphore downloads;

    @Autowired
    public VideoLinkListener(VideoDownloader videoDownloader, BotSender botSender, VideoProperties properties) {
        this(videoDownloader, botSender, properties, Executors.newVirtualThreadPerTaskExecutor());
    }

    VideoLinkListener(VideoDownloader videoDownloader, BotSender botSender, VideoProperties properties, ExecutorService executor) {
        this.videoDownloader = videoDownloader;
        this.botSender = botSender;
        this.properties = properties;
        this.executor = executor;
        this.downloads = new Semaphore(properties.concurrency());
    }

    public boolean onText(long chatId, Integer messageId, String text) {
        if (!properties.enabled()) {
            return false;
        }
        Optional<String> link = VideoLinks.find(text);
        link.ifPresent(url -> executor.execute(() -> deliver(chatId, messageId, url)));
        return link.isPresent();
    }

    private void deliver(long chatId, Integer messageId, String url) {
        try {
            downloads.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            botSender.showUploadingVideo(chatId);
            BotReply reply = videoDownloader.download(url)
                    .<BotReply>map(video -> video)
                    .orElseGet(() -> BotReply.text(DOWNLOAD_FAILED));
            botSender.send(chatId, reply, messageId);
        } catch (RuntimeException e) {
            log.error("Video delivery failed for {} in chat {}", url, chatId, e);
        } finally {
            downloads.release();
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}