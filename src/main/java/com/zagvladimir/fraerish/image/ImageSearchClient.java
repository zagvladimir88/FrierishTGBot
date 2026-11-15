package com.zagvladimir.fraerish.image;

import java.util.List;

public interface ImageSearchClient {

    List<String> findImageUrls(String query, int limit);
}