package com.zagvladimir.fraerish.layout;

import java.util.HashMap;
import java.util.Map;

final class KeyboardLayoutConverter {

    private static final String LATIN = "qwertyuiop[]asdfghjkl;'zxcvbnm,./`"
            + "QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?~"
            + "@#$^&";
    private static final String CYRILLIC = "йцукенгшщзхъфывапролджэячсмитьбю.ё"
            + "ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ,Ё"
            + "\"№;:?";

    private static final Map<Character, Character> LATIN_TO_CYRILLIC = buildMapping();

    private KeyboardLayoutConverter() {
    }

    static String toCyrillic(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (char symbol : text.toCharArray()) {
            result.append(LATIN_TO_CYRILLIC.getOrDefault(symbol, symbol));
        }
        return result.toString();
    }

    private static Map<Character, Character> buildMapping() {
        if (LATIN.length() != CYRILLIC.length()) {
            throw new IllegalStateException("Keyboard layouts must have the same number of keys");
        }
        Map<Character, Character> mapping = new HashMap<>();
        for (int i = 0; i < LATIN.length(); i++) {
            mapping.put(LATIN.charAt(i), CYRILLIC.charAt(i));
        }
        return Map.copyOf(mapping);
    }
}