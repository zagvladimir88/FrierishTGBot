package com.zagvladimir.model;

import lombok.Data;

import java.util.List;

@Data
public class SearchResult {
    private String kind;
    private Url url;
    private Queries queries;
    private Context context;
    private SearchInformation searchInformation;
    private List<Item> items;
}
