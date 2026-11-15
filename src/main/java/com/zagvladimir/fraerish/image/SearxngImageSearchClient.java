package com.zagvladimir.fraerish.image;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

@Component
public class SearxngImageSearchClient implements ImageSearchClient {

    private final RestClient restClient;
    private final int safesearch;

    public SearxngImageSearchClient(RestClient.Builder restClientBuilder, SearxngProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.safesearch = properties.safesearch();
    }

    @Override
    public List<String> findImageUrls(String query, int limit) {
        SearxngSearchResponse response = restClient.get()
                .uri("/search?q={query}&format=json&categories=images&safesearch={safesearch}", query, safesearch)
                .retrieve()
                .body(SearxngSearchResponse.class);
        if (response == null) {
            return List.of();
        }
        return response.resultsOrEmpty().stream()
                .map(SearxngSearchResponse.Result::imageUrl)
                .filter(Objects::nonNull)
                .map(SearxngImageSearchClient::withScheme)
                .filter(url -> url.startsWith("http://") || url.startsWith("https://"))
                .distinct()
                .limit(limit)
                .toList();
    }

    private static String withScheme(String url) {
        return url.startsWith("//") ? "https:" + url : url;
    }
}