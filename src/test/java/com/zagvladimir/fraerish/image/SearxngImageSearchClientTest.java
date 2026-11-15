package com.zagvladimir.fraerish.image;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SearxngImageSearchClientTest {

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final SearxngImageSearchClient client = new SearxngImageSearchClient(
            restClientBuilder, new SearxngProperties("http://searxng.test", 3, 1));

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void returnsDistinctHttpImageUrlsUpToLimit() {
        expectQuery("котики").andRespond(withSuccess("""
                {"query": "котики", "results": [
                  {"url": "https://site.test/page", "img_src": "https://img.test/1.jpg", "engine": "bing images"},
                  {"img_src": null},
                  {"img_src": "data:image/png;base64,AAAA"},
                  {"img_src": "//cdn.test/2.png"},
                  {"img_src": "https://img.test/1.jpg"},
                  {"img_src": "http://img.test/3.jpg"},
                  {"img_src": "https://img.test/4.jpg"}
                ]}""", MediaType.APPLICATION_JSON));

        assertThat(client.findImageUrls("котики", 3))
                .containsExactly("https://img.test/1.jpg", "https://cdn.test/2.png", "http://img.test/3.jpg");
    }

    @Test
    void returnsEmptyListWhenNothingFound() {
        expectQuery("ничего").andRespond(withSuccess("{\"results\": []}", MediaType.APPLICATION_JSON));

        assertThat(client.findImageUrls("ничего", 3)).isEmpty();
    }

    @Test
    void toleratesMissingResults() {
        expectQuery("ничего").andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.findImageUrls("ничего", 3)).isEmpty();
    }

    private ResponseActions expectQuery(String query) {
        return server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/search");
            assertThat(request.getURI().getQuery()).isEqualTo("q=" + query + "&format=json&categories=images&safesearch=1");
        });
    }
}