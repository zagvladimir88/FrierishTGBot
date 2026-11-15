package com.zagvladimir.fraerish.digest;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;

@ConfigurationProperties("bot.digest")
public record DigestProperties(ZoneId zone, String zoneLabel, Duration retryWindow, int maxCities, int maxCurrencies) {
}