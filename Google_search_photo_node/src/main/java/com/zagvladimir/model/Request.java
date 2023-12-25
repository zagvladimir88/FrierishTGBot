package com.zagvladimir.model;

import lombok.Data;

@Data
public class Request {
    private String title;
    private String totalResults;
    private String searchTerms;
    private int count;
    private int startIndex;
    private String inputEncoding;
    private String outputEncoding;
    private String safe;
    private String cx;
    private String searchType;
}
