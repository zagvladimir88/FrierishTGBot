package com.zagvladimir.fraerish.video;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class VideoLinksTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "https://www.youtube.com/shorts/ojsibtSeGfw|https://www.youtube.com/shorts/ojsibtSeGfw",
            "смотри https://youtube.com/shorts/ojsibtSeGfw?si=abc ахах|https://youtube.com/shorts/ojsibtSeGfw",
            "https://www.tiktok.com/@chabiarleiva/video/7568295174619581716?is_from_webapp=1|https://www.tiktok.com/@chabiarleiva/video/7568295174619581716",
            "https://vm.tiktok.com/ZMabc123/|https://vm.tiktok.com/ZMabc123/",
            "https://vt.tiktok.com/ZSxyz/|https://vt.tiktok.com/ZSxyz/",
            "https://www.tiktok.com/t/ZT8abc/|https://www.tiktok.com/t/ZT8abc/",
            "https://www.instagram.com/reels/DeWMDZWBDhX/|https://www.instagram.com/reels/DeWMDZWBDhX/",
            "https://www.instagram.com/reel/DeWMDZWBDhX/?igsh=MWQ1|https://www.instagram.com/reel/DeWMDZWBDhX/"
    })
    void findsSupportedLinkInText(String text, String expected) {
        assertThat(VideoLinks.find(text)).contains(expected);
    }

    @Test
    void takesFirstLinkWhenThereAreSeveral() {
        assertThat(VideoLinks.find("https://vm.tiktok.com/ZMfirst/ и https://www.youtube.com/shorts/second"))
                .contains("https://vm.tiktok.com/ZMfirst/");
        assertThat(VideoLinks.find("https://www.youtube.com/shorts/first и https://vm.tiktok.com/ZMsecond/"))
                .contains("https://www.youtube.com/shorts/first");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "просто текст",
            "https://www.youtube.com/watch?v=p6GKKdwufOk",
            "https://www.instagram.com/instagram/",
            "https://www.tiktok.com/@tiktok",
            "https://example.com/shorts/abc"
    })
    void ignoresUnsupportedLinks(String text) {
        assertThat(VideoLinks.find(text)).isEmpty();
    }
}