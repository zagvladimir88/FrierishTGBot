package com.zagvladimir.fraerish.command;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ListInput {

    private static final Pattern CITY_SEPARATOR = Pattern.compile("[,;]");
    private static final Pattern CURRENCY_SEPARATOR = Pattern.compile("[,;\\s]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private ListInput() {
    }

    public static List<String> cities(String input) {
        return distinctIgnoringCase(Arrays.stream(CITY_SEPARATOR.split(input))
                .map(city -> WHITESPACE.matcher(city.strip()).replaceAll(" "))
                .toList());
    }

    public static List<String> currencies(String input) {
        return distinctIgnoringCase(Arrays.stream(CURRENCY_SEPARATOR.split(input.strip()))
                .map(code -> code.toUpperCase(Locale.ROOT))
                .toList());
    }

    public static List<String> distinctIgnoringCase(List<String> values) {
        Map<String, String> unique = new LinkedHashMap<>();
        values.stream()
                .filter(value -> !value.isBlank())
                .forEach(value -> unique.putIfAbsent(value.toLowerCase(Locale.ROOT), value));
        return List.copyOf(unique.values());
    }
}