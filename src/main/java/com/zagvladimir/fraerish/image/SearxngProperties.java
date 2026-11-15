package com.zagvladimir.fraerish.image;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.searxng")
public record SearxngProperties(String baseUrl, int maxImages, int safesearch) {
}