package com.zagvladimir.fraerish.image;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class TestImages {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
    static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);
    static final byte[] HTML = "<!doctype html><html></html>".getBytes(StandardCharsets.US_ASCII);

    private TestImages() {
    }

    static byte[] pngOfSize(int size) {
        byte[] content = Arrays.copyOf(PNG, size);
        Arrays.fill(content, PNG.length, size, (byte) 7);
        return content;
    }
}