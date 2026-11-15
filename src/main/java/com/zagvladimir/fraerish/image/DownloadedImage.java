package com.zagvladimir.fraerish.image;

import java.util.Optional;

public record DownloadedImage(ImageFormat format, byte[] content) {

    static Optional<DownloadedImage> of(byte[] content) {
        return ImageFormat.detect(content).map(format -> new DownloadedImage(format, content));
    }
}