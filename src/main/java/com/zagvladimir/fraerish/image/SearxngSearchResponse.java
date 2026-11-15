package com.zagvladimir.fraerish.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record SearxngSearchResponse(List<Result> results) {

    List<Result> resultsOrEmpty() {
        return results == null ? List.of() : results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(@JsonProperty("img_src") String imageUrl, @JsonProperty("thumbnail_src") String thumbnailUrl) {
    }
}