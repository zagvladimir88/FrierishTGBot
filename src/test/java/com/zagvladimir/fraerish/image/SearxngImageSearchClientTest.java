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
            restClientBuilder, new SearxngProperties("http://searxng.test", 10, 3, 1));

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void returnsDistinctCandidatesWithHttpSourcesUpToLimit() {
        expectQuery("котики").andRespond(withSuccess("""
                {"query": "котики", "results": [
                  {"url": "https://site.test/page", "img_src": "https://img.test/1.jpg", "thumbnail_src": "https://thumb.test/1.jpg"},
                  {"img_src": null, "thumbnail_src": null},
                  {"img_src": "data:image/png;base64,AAAA", "thumbnail_src": "//thumb.test/2.jpg"},
                  {"img_src": "https://img.test/1.jpg", "thumbnail_src": "https://thumb.test/1.jpg"},
                  {"img_src": "http://img.test/3.jpg"},
                  {"img_src": "https://img.test/4.jpg"}
                ]}""", MediaType.APPLICATION_JSON));

        assertThat(client.search("котики", 3)).containsExactly(
                new ImageCandidate("https://img.test/1.jpg", "https://thumb.test/1.jpg"),
                new ImageCandidate(null, "https://thumb.test/2.jpg"),
                new ImageCandidate("http://img.test/3.jpg", null));
    }

    @Test
    void returnsEmptyListWhenNothingFound() {
        expectQuery("ничего").andRespond(withSuccess("{\"results\": []}", MediaType.APPLICATION_JSON));

        assertThat(client.search("ничего", 3)).isEmpty();
    }

    @Test
    void toleratesMissingResults() {
        expectQuery("ничего").andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.search("ничего", 3)).isEmpty();
    }

    private ResponseActions expectQuery(String query) {
        return server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/search");
            assertThat(request.getURI().getQuery()).isEqualTo("q=" + query + "&format=json&categories=images&safesearch=1");
        });
    }
}