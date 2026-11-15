package com.zagvladimir.fraerish.image;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ImageDownloaderTest {

    private static final ImageDownloadProperties PROPERTIES = new ImageDownloadProperties(
            Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(5), DataSize.ofBytes(1024));

    private final ImageDownloader downloader = new ImageDownloader(PROPERTIES, uri -> true);
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/cat.png", exchange -> respond(exchange, 200, TestImages.PNG));
        server.createContext("/cat-without-type", exchange -> respond(exchange, 200, TestImages.JPEG));
        server.createContext("/page", exchange -> respond(exchange, 200, TestImages.HTML));
        server.createContext("/missing", exchange -> respond(exchange, 404, new byte[0]));
        server.createContext("/huge.png", exchange -> respond(exchange, 200, TestImages.pngOfSize(2048)));
        server.createContext("/limit.png", exchange -> respond(exchange, 200, TestImages.pngOfSize(1024)));
        server.createContext("/moved", exchange -> redirect(exchange, "/cat.png"));
        server.createContext("/loop", exchange -> redirect(exchange, "/loop"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsImageAndDetectsFormat() {
        DownloadedImage image = downloader.download(url("/cat.png")).orElseThrow();

        assertThat(image.format()).isEqualTo(ImageFormat.PNG);
        assertThat(image.content()).isEqualTo(TestImages.PNG);
    }

    @Test
    void detectsFormatByContentNotByUrl() {
        assertThat(downloader.download(url("/cat-without-type"))).map(DownloadedImage::format).contains(ImageFormat.JPEG);
    }

    @Test
    void skipsPagesThatAreNotImages() {
        assertThat(downloader.download(url("/page"))).isEmpty();
    }

    @Test
    void skipsErrorResponses() {
        assertThat(downloader.download(url("/missing"))).isEmpty();
    }

    @Test
    void skipsImagesOverSizeLimit() {
        assertThat(downloader.download(url("/huge.png"))).isEmpty();
        assertThat(downloader.download(url("/limit.png"))).isPresent();
    }

    @Test
    void followsRedirectsButNotEndlessly() {
        assertThat(downloader.download(url("/moved"))).map(DownloadedImage::format).contains(ImageFormat.PNG);
        assertThat(downloader.download(url("/loop"))).isEmpty();
    }

    @Test
    void skipsMalformedAndNonHttpUrls() {
        assertThat(downloader.download("not a url")).isEmpty();
        assertThat(downloader.download("ftp://127.0.0.1/cat.png")).isEmpty();
    }

    @Test
    void refusesInternalHostsByDefault() {
        ImageDownloader publicOnly = new ImageDownloader(PROPERTIES);

        assertThat(publicOnly.download(url("/cat.png"))).isEmpty();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        respond(exchange, 302, new byte[0]);
    }
}