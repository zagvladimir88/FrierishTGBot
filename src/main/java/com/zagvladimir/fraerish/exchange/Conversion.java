package com.zagvladimir.fraerish.exchange;

import com.zagvladimir.fraerish.command.ListInput;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record Conversion(BigDecimal amount, String from, List<String> targets) {

    private static final Pattern REQUEST = Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s*([$€₽₿]|\\p{L}[\\p{L}\\d]{1,9})(?:\\s+(.*))?$", Pattern.DOTALL);
    private static final Map<String, String> SYMBOLS = Map.of("$", "USD", "€", "EUR", "₽", "RUB", "₿", "BTC");
    private static final Set<String> FILLER_WORDS = Set.of("В", "ВО", "TO", "IN", "=");

    static Optional<Conversion> parse(String args) {
        Matcher matcher = REQUEST.matcher(args.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String from = currency(matcher.group(2));
        List<String> targets = matcher.group(3) == null ? List.of() : ListInput.currencies(matcher.group(3)).stream()
                .map(Conversion::currency)
                .filter(code -> !FILLER_WORDS.contains(code))
                .filter(code -> !code.equals(from))
                .toList();
        BigDecimal amount = new BigDecimal(matcher.group(1).replace(',', '.'));
        return Optional.of(new Conversion(amount, from, targets));
    }

    private static String currency(String token) {
        return SYMBOLS.getOrDefault(token, token.toUpperCase(Locale.ROOT));
    }
}