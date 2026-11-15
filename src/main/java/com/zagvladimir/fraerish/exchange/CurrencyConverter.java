package com.zagvladimir.fraerish.exchange;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrencyConverter {

    static final String BYN = "BYN";
    private static final List<String> TARGETS_FOR_BYN = List.of("USD", "EUR");

    private final RateService rateService;

    String convert(Conversion conversion) {
        Optional<BigDecimal> bynPerUnit = bynPerUnit(conversion.from());
        if (bynPerUnit.isEmpty()) {
            return explain(conversion.from());
        }
        BigDecimal inByn = conversion.amount().multiply(bynPerUnit.get());
        List<String> targets = conversion.targets().isEmpty() ? defaultTargets(conversion.from()) : conversion.targets();
        List<String> lines = new ArrayList<>();
        lines.add("%s %s =".formatted(format(conversion.amount()), conversion.from()));
        for (String target : targets) {
            lines.add(bynPerUnit(target)
                    .map(rate -> "%s %s".formatted(format(inByn.divide(rate, MathContext.DECIMAL64)), target))
                    .orElseGet(() -> explain(target)));
        }
        return String.join("\n", lines);
    }

    private Optional<BigDecimal> bynPerUnit(String code) {
        if (BYN.equals(code)) {
            return Optional.of(BigDecimal.ONE);
        }
        return switch (rateService.quote(code)) {
            case Quote.Fiat fiat -> Optional.of(fiat.rateInByn().divide(BigDecimal.valueOf(fiat.scale()), MathContext.DECIMAL64));
            case Quote.Crypto crypto -> Optional.ofNullable(crypto.priceInByn());
            case Quote.Unknown unknown -> Optional.empty();
            case Quote.Unavailable unavailable -> Optional.empty();
        };
    }

    private String explain(String code) {
        return QuoteFormatter.format(rateService.quote(code));
    }

    private static List<String> defaultTargets(String from) {
        return BYN.equals(from) ? TARGETS_FOR_BYN : List.of(BYN);
    }

    static String format(BigDecimal value) {
        BigDecimal absolute = value.abs();
        int fractionDigits = absolute.compareTo(BigDecimal.ONE) >= 0 ? 2 : 6;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0." + "#".repeat(fractionDigits), symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }
}