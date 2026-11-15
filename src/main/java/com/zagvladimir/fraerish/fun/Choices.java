package com.zagvladimir.fraerish.fun;

import com.zagvladimir.fraerish.command.ListInput;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

final class Choices {

    private static final Pattern SEPARATOR = Pattern.compile("\\s*(?:[,;\\n]|\\s+или\\s+|\\s+or\\s+)\\s*", Pattern.CASE_INSENSITIVE);

    private Choices() {
    }

    static List<String> parse(String text) {
        return ListInput.distinctIgnoringCase(Arrays.stream(SEPARATOR.split(text.strip()))
                .map(String::strip)
                .filter(choice -> !choice.isEmpty())
                .toList());
    }
}