package com.zagvladimir.fraerish.image;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class SearxngImageSearchClient implements ImageSearchClient {

    private final RestClient restClient;
    private final int safesearch;

    public SearxngImageSearchClient(RestClient.Builder restClientBuilder, SearxngProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.safesearch = properties.safesearch();
    }

    @Override
    public List<ImageCandidate> search(String query, int limit) {
        SearxngSearchResponse response = restClient.get()
                .uri("/search?q={query}&format=json&categories=images&safesearch={safesearch}", query, safesearch)
                .retrieve()
                .body(SearxngSearchResponse.class);
        if (response == null) {
            return List.of();
        }
        return response.resultsOrEmpty().stream()
                .map(result -> new ImageCandidate(httpUrlOrNull(result.imageUrl()), httpUrlOrNull(result.thumbnailUrl())))
                .filter(ImageCandidate::hasSources)
                .distinct()
                .limit(limit)
                .toList();
    }

    private static String httpUrlOrNull(String url) {
        if (url == null) {
            return null;
        }
        String withScheme = url.startsWith("//") ? "https:" + url : url;
        return withScheme.startsWith("http://") || withScheme.startsWith("https://") ? withScheme : null;
    }
}