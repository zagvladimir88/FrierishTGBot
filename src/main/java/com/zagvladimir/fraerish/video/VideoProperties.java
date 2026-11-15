package com.zagvladimir.fraerish.video;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties("bot.video")
public record VideoProperties(boolean enabled, String ytDlpPath, Duration timeout, DataSize maxSize, int maxHeight, int concurrency) {
}