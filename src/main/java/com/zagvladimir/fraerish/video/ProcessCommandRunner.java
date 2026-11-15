package com.zagvladimir.fraerish.video;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessCommandRunner implements CommandRunner {

    @Override
    public Optional<CommandResult> run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        Path output = Files.createTempFile("command-", ".log");
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            return Optional.of(new CommandResult(process.exitValue(), Files.readString(output, StandardCharsets.UTF_8)));
        } finally {
            Files.deleteIfExists(output);
        }
    }
}