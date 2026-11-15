package com.zagvladimir.fraerish.image;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class PublicHostPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/a.jpg",
            "http://0.0.0.0/a.jpg",
            "http://10.8.0.1/a.jpg",
            "http://172.18.0.2:8080/a.jpg",
            "http://192.168.2.1/a.jpg",
            "http://169.254.169.254/latest/meta-data",
            "http://[::1]/a.jpg",
            "http://[fd42:42:42::1]/a.jpg",
            "file:///etc/passwd"
    })
    void rejectsInternalAddresses(String url) {
        assertThat(PublicHostPolicy.allows(URI.create(url))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://8.8.8.8/a.jpg", "https://[2606:4700:4700::1111]/a.jpg"})
    void allowsPublicAddresses(String url) {
        assertThat(PublicHostPolicy.allows(URI.create(url))).isTrue();
    }
}