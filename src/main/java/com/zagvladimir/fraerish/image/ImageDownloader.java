package com.zagvladimir.fraerish.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
@Component
public class ImageDownloader {

    private static final int MAX_REDIRECTS = 3;
    private static final int OK = 200;
    private static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; FraerishBot/2.0)";

    private final HttpClient httpClient;
    private final Duration readTimeout;
    private final int maxSizeBytes;
    private final Predicate<URI> hostPolicy;

    @Autowired
    public ImageDownloader(ImageDownloadProperties properties) {
        this(properties, PublicHostPolicy::allows);
    }

    ImageDownloader(ImageDownloadProperties properties, Predicate<URI> hostPolicy) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.readTimeout = properties.readTimeout();
        this.maxSizeBytes = Math.toIntExact(properties.maxSize().toBytes());
        this.hostPolicy = hostPolicy;
    }

    public Optional<DownloadedImage> download(String url) {
        try {
            return fetch(URI.create(url)).flatMap(DownloadedImage::of);
        } catch (IOException | IllegalArgumentException e) {
            log.debug("Image {} is not available: {}", url, e.toString());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<byte[]> fetch(URI uri) throws IOException, InterruptedException {
        URI current = uri;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            if (!isAllowed(current)) {
                return Optional.empty();
            }
            HttpResponse<InputStream> response = httpClient.send(request(current), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (isRedirect(response.statusCode())) {
                    Optional<String> location = response.headers().firstValue("Location");
                    if (location.isEmpty()) {
                        return Optional.empty();
                    }
                    current = current.resolve(location.get());
                    continue;
                }
                return response.statusCode() == OK ? readWithinLimit(body) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    private HttpRequest request(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(readTimeout)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "image/jpeg,image/png,image/webp,image/*;q=0.8")
                .GET()
                .build();
    }

    private boolean isAllowed(URI uri) {
        return uri.getScheme() != null
                && SUPPORTED_SCHEMES.contains(uri.getScheme().toLowerCase())
                && hostPolicy.test(uri);
    }

    private Optional<byte[]> readWithinLimit(InputStream body) throws IOException {
        byte[] content = body.readNBytes(maxSizeBytes + 1);
        return content.length > maxSizeBytes ? Optional.empty() : Optional.of(content);
    }

    private static boolean isRedirect(int status) {
        return status >= 300 && status < 400;
    }
}