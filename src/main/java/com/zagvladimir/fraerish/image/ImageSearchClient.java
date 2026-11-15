package com.zagvladimir.fraerish.image;

import java.util.List;

public interface ImageSearchClient {

    List<ImageCandidate> search(String query, int limit);
}