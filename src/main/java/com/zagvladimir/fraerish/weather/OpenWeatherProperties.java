package com.zagvladimir.fraerish.weather;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.open-weather")
public record OpenWeatherProperties(String baseUrl, String apiKey) {
}