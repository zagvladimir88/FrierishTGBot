package com.zagvladimir.model;

import lombok.Data;

@Data
public class Image {
    private String contextLink;
    private int height;
    private int width;
    private int byteSize;
    private String thumbnailLink;
    private int thumbnailHeight;
    private int thumbnailWidth;
}
