package com.zagvladimir.fraerish.exchange;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.binance")
public record BinanceProperties(String baseUrl) {
}