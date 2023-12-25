package com.zagvladimir.model;

import lombok.Data;

@Data
public class Item {
    private String kind;
    private String title;
    private String htmlTitle;
    private String link;
    private String displayLink;
    private String snippet;
    private String htmlSnippet;
    private String mime;
    private String fileFormat;
    private Image image;
}
