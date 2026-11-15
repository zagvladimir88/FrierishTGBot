package com.zagvladimir.fraerish.video;

import com.zagvladimir.fraerish.command.BotReply;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VideoDownloaderTest {

    private static final VideoProperties PROPERTIES = new VideoProperties(true, "yt-dlp", Duration.ofSeconds(30), DataSize.ofBytes(1024), 720, 2);
    private static final String URL = "https://www.youtube.com/shorts/ojsibtSeGfw";

    private final List<List<String>> commands = new ArrayList<>();
    private final List<Path> directories = new ArrayList<>();

    @Test
    void downloadsVideoWithMetadataAndCleansUp() {
        VideoDownloader downloader = downloader(writing(new byte[]{1, 2, 3}, 0, "META|1080|1920|58.6"));

        Optional<BotReply.Video> video = downloader.download(URL);

        assertThat(video).hasValueSatisfying(result -> {
            assertThat(result.fileName()).isEqualTo("video.mp4");
            assertThat(result.content()).containsExactly(1, 2, 3);
            assertThat(result.width()).isEqualTo(1080);
            assertThat(result.height()).isEqualTo(1920);
            assertThat(result.duration()).isEqualTo(59);
        });
        assertThat(directories).allMatch(directory -> Files.notExists(directory));
    }

    @Test
    void asksForTelegramFriendlyMp4WithinLimits() {
        downloader(writing(new byte[]{1}, 0, "")).download(URL);

        List<String> command = commands.getFirst();
        assertThat(command.getFirst()).isEqualTo("yt-dlp");
        assertThat(command.getLast()).isEqualTo(URL);
        assertThat(command).containsSequence("-S", "vcodec:h264,res:720,acodec:m4a")
                .containsSequence("--merge-output-format", "mp4")
                .containsSequence("--max-filesize", "1024")
                .contains("--no-playlist");
    }

    @Test
    void sendsVideoWithoutMetadataWhenYtDlpPrintsNone() {
        Optional<BotReply.Video> video = downloader(writing(new byte[]{1}, 0, "")).download(URL);

        assertThat(video).hasValueSatisfying(result -> assertThat(result.width()).isNull());
    }

    @Test
    void failsOnErrorTimeoutOrOversizedFile() {
        assertThat(downloader(writing(new byte[]{1}, 1, "ERROR: Unsupported URL")).download(URL)).isEmpty();
        assertThat(downloader((command, timeout) -> Optional.empty()).download(URL)).isEmpty();
        assertThat(downloader(writing(new byte[2048], 0, "")).download(URL)).isEmpty();
        assertThat(downloader((command, timeout) -> Optional.of(new CommandRunner.CommandResult(0, ""))).download(URL)).isEmpty();
    }

    private VideoDownloader downloader(CommandRunner runner) {
        return new VideoDownloader((command, timeout) -> {
            commands.add(command);
            directories.add(Path.of(command.get(command.indexOf("-o") + 1)).getParent());
            return runner.run(command, timeout);
        }, PROPERTIES);
    }

    private static CommandRunner writing(byte[] content, int exitCode, String output) {
        return (command, timeout) -> {
            String template = command.get(command.indexOf("-o") + 1);
            Files.write(Path.of(template.replace("%(ext)s", "mp4")), content);
            return Optional.of(new CommandRunner.CommandResult(exitCode, output));
        };
    }
}