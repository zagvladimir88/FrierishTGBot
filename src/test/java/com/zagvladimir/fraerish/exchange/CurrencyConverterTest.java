package com.zagvladimir.fraerish.exchange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrencyConverterTest {

    private final RateService rateService = mock(RateService.class);
    private final CurrencyConverter converter = new CurrencyConverter(rateService);

    @BeforeEach
    void rates() {
        when(rateService.quote(anyString())).thenAnswer(invocation -> new Quote.Unknown(invocation.getArgument(0)));
        when(rateService.quote("USD")).thenReturn(new Quote.Fiat("USD", 1, new BigDecimal("3.0000")));
        when(rateService.quote("EUR")).thenReturn(new Quote.Fiat("EUR", 1, new BigDecimal("3.5000")));
        when(rateService.quote("RUB")).thenReturn(new Quote.Fiat("RUB", 100, new BigDecimal("3.7000")));
        when(rateService.quote("BTC")).thenReturn(new Quote.Crypto("BTC", new BigDecimal("65000"), new BigDecimal("195000")));
    }

    @Test
    void parsesAmountCurrencyAndTargets() {
        assertThat(Conversion.parse("100 usd eur")).contains(new Conversion(new BigDecimal("100"), "USD", List.of("EUR")));
        assertThat(Conversion.parse("100,5 usd в eur, rub")).contains(new Conversion(new BigDecimal("100.5"), "USD", List.of("EUR", "RUB")));
        assertThat(Conversion.parse("50$")).contains(new Conversion(new BigDecimal("50"), "USD", List.of()));
        assertThat(Conversion.parse("0.5 btc")).contains(new Conversion(new BigDecimal("0.5"), "BTC", List.of()));
        assertThat(Conversion.parse("USD, EUR")).isEmpty();
        assertThat(Conversion.parse("100")).isEmpty();
    }

    @Test
    void convertsToBynByDefault() {
        assertThat(converter.convert(Conversion.parse("100 usd").orElseThrow())).isEqualTo("100 USD =\n300 BYN");
    }

    @Test
    void convertsBetweenFiatCurrenciesThroughByn() {
        assertThat(converter.convert(Conversion.parse("100 usd eur, rub").orElseThrow()))
                .isEqualTo("100 USD =\n85.71 EUR\n8 108.11 RUB");
    }

    @Test
    void convertsCryptoAndSmallAmounts() {
        assertThat(converter.convert(Conversion.parse("0.5 btc usd").orElseThrow())).isEqualTo("0.5 BTC =\n32 500 USD");
        assertThat(converter.convert(Conversion.parse("100 usd btc").orElseThrow())).isEqualTo("100 USD =\n0.001538 BTC");
    }

    @Test
    void convertsFromBynToUsdAndEurByDefault() {
        assertThat(converter.convert(Conversion.parse("300 byn").orElseThrow())).isEqualTo("300 BYN =\n100 USD\n85.71 EUR");
    }

    @Test
    void explainsUnknownCurrencies() {
        assertThat(converter.convert(Conversion.parse("100 xyz").orElseThrow())).isEqualTo("XYZ — не знаю такую валюту");
        assertThat(converter.convert(Conversion.parse("100 usd xyz").orElseThrow())).isEqualTo("100 USD =\nXYZ — не знаю такую валюту");
    }
}