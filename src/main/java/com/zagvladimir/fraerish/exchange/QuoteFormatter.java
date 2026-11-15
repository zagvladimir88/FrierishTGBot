package com.zagvladimir.fraerish.exchange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class QuoteFormatter {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private QuoteFormatter() {
    }

    public static String format(Quote quote) {
        return switch (quote) {
            case Quote.Fiat fiat -> "%s%s — %s BYN".formatted(
                    fiat.scale() == 1 ? "" : fiat.scale() + " ", fiat.code(), number(fiat.rateInByn(), 2));
            case Quote.Crypto crypto -> crypto.priceInByn() == null
                    ? "%s — %s $".formatted(crypto.code(), price(crypto.priceInUsd()))
                    : "%s — %s $ (≈ %s BYN)".formatted(crypto.code(), price(crypto.priceInUsd()), price(crypto.priceInByn()));
            case Quote.Unknown unknown -> unknown.code() + " — не знаю такую валюту";
            case Quote.Unavailable unavailable -> unavailable.code() + " — курс сейчас недоступен";
        };
    }

    private static String price(BigDecimal value) {
        if (value.compareTo(HUNDRED) >= 0) {
            return number(value, 0);
        }
        return number(value, value.compareTo(BigDecimal.ONE) >= 0 ? 2 : 4);
    }

    private static String number(BigDecimal value, int fractionDigits) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat(fractionDigits == 0 ? "#,##0" : "#,##0." + "0".repeat(fractionDigits), symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }
}