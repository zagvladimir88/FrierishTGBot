package com.zagvladimir.fraerish.video;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VideoLinks {

    private static final List<Pattern> SUPPORTED = List.of(
            Pattern.compile("https?://(?:www\\.|m\\.)?tiktok\\.com/@[\\w.-]+/video/\\d+"),
            Pattern.compile("https?://(?:vm|vt)\\.tiktok\\.com/[\\w-]+/?"),
            Pattern.compile("https?://(?:www\\.)?tiktok\\.com/t/[\\w-]+/?"),
            Pattern.compile("https?://(?:www\\.)?instagram\\.com/(?:reel|reels)/[\\w-]+/?"),
            Pattern.compile("https?://(?:www\\.|m\\.)?youtube\\.com/shorts/[\\w-]+"));

    private VideoLinks() {
    }

    static Optional<String> find(String text) {
        if (text == null) {
            return Optional.empty();
        }
        return SUPPORTED.stream()
                .map(pattern -> pattern.matcher(text))
                .filter(Matcher::find)
                .min((left, right) -> Integer.compare(left.start(), right.start()))
                .map(Matcher::group);
    }
}