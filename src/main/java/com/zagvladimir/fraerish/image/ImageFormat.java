package com.zagvladimir.fraerish.image;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public enum ImageFormat {

    JPEG("jpg", content -> startsWith(content, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})),
    PNG("png", content -> startsWith(content, 0, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})),
    WEBP("webp", content -> startsWith(content, 0, ascii("RIFF")) && startsWith(content, 8, ascii("WEBP")));

    private final String extension;
    private final Predicate<byte[]> signature;

    ImageFormat(String extension, Predicate<byte[]> signature) {
        this.extension = extension;
        this.signature = signature;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ImageFormat> detect(byte[] content) {
        return Arrays.stream(values()).filter(format -> format.signature.test(content)).findFirst();
    }

    private static boolean startsWith(byte[] content, int offset, byte[] prefix) {
        return content.length >= offset + prefix.length
                && Arrays.equals(content, offset, offset + prefix.length, prefix, 0, prefix.length);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }
}