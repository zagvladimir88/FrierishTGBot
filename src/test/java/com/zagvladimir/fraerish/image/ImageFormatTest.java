package com.zagvladimir.fraerish.image;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageFormatTest {

    @Test
    void detectsFormatBySignature() {
        assertThat(ImageFormat.detect(TestImages.JPEG)).contains(ImageFormat.JPEG);
        assertThat(ImageFormat.detect(TestImages.PNG)).contains(ImageFormat.PNG);
        assertThat(ImageFormat.detect(TestImages.WEBP)).contains(ImageFormat.WEBP);
    }

    @Test
    void rejectsContentThatIsNotSupportedImage() {
        assertThat(ImageFormat.detect(TestImages.HTML)).isEmpty();
        assertThat(ImageFormat.detect(new byte[0])).isEmpty();
        assertThat(ImageFormat.detect(new byte[]{(byte) 0xFF, (byte) 0xD8})).isEmpty();
        assertThat(ImageFormat.detect("GIF89a".getBytes())).isEmpty();
    }
}