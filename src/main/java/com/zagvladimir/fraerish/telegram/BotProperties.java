package com.zagvladimir.fraerish.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bot")
public record BotProperties(String token, String name) {
}