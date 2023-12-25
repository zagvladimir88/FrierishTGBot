package com.zagvladimir.model;

import lombok.Data;

@Data
public class SearchInformation {
    private double searchTime;
    private String formattedSearchTime;
    private String totalResults;
    private String formattedTotalResults;
}
