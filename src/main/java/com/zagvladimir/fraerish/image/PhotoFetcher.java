package com.zagvladimir.fraerish.image;

import com.zagvladimir.fraerish.command.BotReply;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class PhotoFetcher {

    private final ImageDownloader imageDownloader;
    private final Duration totalTimeout;

    public PhotoFetcher(ImageDownloader imageDownloader, ImageDownloadProperties properties) {
        this.imageDownloader = imageDownloader;
        this.totalTimeout = properties.totalTimeout();
    }

    public List<BotReply.Photo> fetch(List<ImageCandidate> candidates, int limit) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            List<Future<Optional<DownloadedImage>>> downloads = candidates.stream()
                    .map(candidate -> executor.submit(() -> downloadFirstAvailable(candidate)))
                    .toList();
            return collectInOrder(downloads, limit, System.nanoTime() + totalTimeout.toNanos());
        } finally {
            executor.shutdownNow();
        }
    }

    private Optional<DownloadedImage> downloadFirstAvailable(ImageCandidate candidate) {
        return candidate.sources().stream()
                .map(imageDownloader::download)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static List<BotReply.Photo> collectInOrder(List<Future<Optional<DownloadedImage>>> downloads, int limit, long deadline) {
        List<BotReply.Photo> photos = new ArrayList<>(limit);
        for (Future<Optional<DownloadedImage>> download : downloads) {
            if (photos.size() == limit) {
                break;
            }
            await(download, deadline).ifPresent(image -> photos.add(toPhoto(image, photos.size() + 1)));
        }
        return photos;
    }

    private static Optional<DownloadedImage> await(Future<Optional<DownloadedImage>> download, long deadline) {
        try {
            return download.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException | ExecutionException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static BotReply.Photo toPhoto(DownloadedImage image, int number) {
        return new BotReply.Photo("image-%d.%s".formatted(number, image.format().extension()), image.content());
    }
}