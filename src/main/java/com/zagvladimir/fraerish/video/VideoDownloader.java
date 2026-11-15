package com.zagvladimir.fraerish.video;

import com.zagvladimir.fraerish.command.BotReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoDownloader {

    private static final String FILE_NAME = "video";
    private static final String METADATA_PREFIX = "META|";

    private final CommandRunner commandRunner;
    private final VideoProperties properties;

    public Optional<BotReply.Video> download(String url) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("video-");
            Optional<CommandRunner.CommandResult> result = commandRunner.run(command(url, directory), properties.timeout());
            if (result.isEmpty()) {
                log.warn("yt-dlp timed out for {}", url);
                return Optional.empty();
            }
            if (result.get().exitCode() != 0) {
                log.warn("yt-dlp failed for {}: {}", url, lastLine(result.get().output()));
                return Optional.empty();
            }
            return readVideo(directory, result.get().output());
        } catch (IOException e) {
            log.warn("Could not download {}: {}", url, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            deleteQuietly(directory);
        }
    }

    private List<String> command(String url, Path directory) {
        return List.of(
                properties.ytDlpPath(),
                "--no-playlist",
                "--no-warnings",
                "--no-cache-dir",
                "--quiet",
                "--no-progress",
                "-f", "bv*+ba/b",
                "-S", "vcodec:h264,res:%d,acodec:m4a".formatted(properties.maxHeight()),
                "--merge-output-format", "mp4",
                "--max-filesize", String.valueOf(properties.maxSize().toBytes()),
                "--print", "after_move:" + METADATA_PREFIX + "%(width)s|%(height)s|%(duration)s",
                "-o", directory.resolve(FILE_NAME + ".%(ext)s").toString(),
                url);
    }

    private Optional<BotReply.Video> readVideo(Path directory, String output) throws IOException {
        Optional<Path> file;
        try (Stream<Path> files = Files.list(directory)) {
            file = files.filter(path -> path.getFileName().toString().startsWith(FILE_NAME + ".")).findFirst();
        }
        if (file.isEmpty() || Files.size(file.get()) > properties.maxSize().toBytes()) {
            log.warn("yt-dlp produced no video within {}", properties.maxSize());
            return Optional.empty();
        }
        Metadata metadata = Metadata.parse(output);
        return Optional.of(new BotReply.Video(file.get().getFileName().toString(), Files.readAllBytes(file.get()),
                metadata.width(), metadata.height(), metadata.duration()));
    }

    private static String lastLine(String output) {
        String[] lines = output.strip().split("\\R");
        return lines[lines.length - 1];
    }

    private static void deleteQuietly(Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            log.warn("Could not clean up {}: {}", directory, e.getMessage());
        }
    }

    record Metadata(Integer width, Integer height, Integer duration) {

        static Metadata parse(String output) {
            return output.lines()
                    .filter(line -> line.startsWith(METADATA_PREFIX))
                    .reduce((first, second) -> second)
                    .map(line -> line.substring(METADATA_PREFIX.length()).split("\\|"))
                    .filter(parts -> parts.length == 3)
                    .map(parts -> new Metadata(number(parts[0]), number(parts[1]), number(parts[2])))
                    .orElse(new Metadata(null, null, null));
        }

        private static Integer number(String value) {
            try {
                return (int) Math.round(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}