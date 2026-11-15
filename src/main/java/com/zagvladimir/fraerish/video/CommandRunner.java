package com.zagvladimir.fraerish.video;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface CommandRunner {

    Optional<CommandResult> run(List<String> command, Duration timeout) throws IOException, InterruptedException;

    record CommandResult(int exitCode, String output) {
    }
}