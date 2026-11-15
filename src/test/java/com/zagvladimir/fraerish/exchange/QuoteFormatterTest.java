package com.zagvladimir.fraerish.exchange;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteFormatterTest {

    @Test
    void formatsFiatRateWithScale() {
        assertThat(QuoteFormatter.format(new Quote.Fiat("USD", 1, new BigDecimal("3.0026")))).isEqualTo("USD — 3.00 BYN");
        assertThat(QuoteFormatter.format(new Quote.Fiat("RUB", 100, new BigDecimal("3.7065")))).isEqualTo("100 RUB — 3.71 BYN");
    }

    @Test
    void formatsCryptoPriceDependingOnMagnitude() {
        assertThat(QuoteFormatter.format(new Quote.Crypto("BTC", new BigDecimal("65080.01"), new BigDecimal("195413.24"))))
                .isEqualTo("BTC — 65 080 $ (≈ 195 413 BYN)");
        assertThat(QuoteFormatter.format(new Quote.Crypto("TON", new BigDecimal("5.4321"), new BigDecimal("16.31"))))
                .isEqualTo("TON — 5.43 $ (≈ 16.31 BYN)");
        assertThat(QuoteFormatter.format(new Quote.Crypto("DOGE", new BigDecimal("0.153456"), null)))
                .isEqualTo("DOGE — 0.1535 $");
    }

    @Test
    void explainsUnknownAndUnavailableCurrencies() {
        assertThat(QuoteFormatter.format(new Quote.Unknown("XYZ"))).isEqualTo("XYZ — не знаю такую валюту");
        assertThat(QuoteFormatter.format(new Quote.Unavailable("USD"))).isEqualTo("USD — курс сейчас недоступен");
    }
}