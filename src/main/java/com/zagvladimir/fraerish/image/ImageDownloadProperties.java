package com.zagvladimir.fraerish.image;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties("integration.image-download")
public record ImageDownloadProperties(Duration connectTimeout, Duration readTimeout, Duration totalTimeout, DataSize maxSize) {
}