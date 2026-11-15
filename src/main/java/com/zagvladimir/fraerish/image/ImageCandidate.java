package com.zagvladimir.fraerish.image;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record ImageCandidate(String imageUrl, String thumbnailUrl) {

    public List<String> sources() {
        return Stream.of(imageUrl, thumbnailUrl).filter(Objects::nonNull).toList();
    }

    public boolean hasSources() {
        return imageUrl != null || thumbnailUrl != null;
    }
}