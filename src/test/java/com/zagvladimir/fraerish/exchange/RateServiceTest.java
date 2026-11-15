package com.zagvladimir.fraerish.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RateServiceTest {

    private final NbrbClient nbrbClient = mock(NbrbClient.class);
    private final CryptoPriceClient cryptoPriceClient = mock(CryptoPriceClient.class);
    private final RateService rateService = new RateService(nbrbClient, cryptoPriceClient);

    @Test
    void prefersOfficialNbrbRate() {
        when(nbrbClient.find("EUR")).thenReturn(Optional.of(new ExchangeRate(1, new BigDecimal("3.4812"))));

        assertThat(rateService.quote("EUR")).isEqualTo(new Quote.Fiat("EUR", 1, new BigDecimal("3.4812")));
        verifyNoInteractions(cryptoPriceClient);
    }

    @Test
    void fallsBackToCryptoAndConvertsToByn() {
        when(nbrbClient.find("BTC")).thenReturn(Optional.empty());
        when(nbrbClient.find("USD")).thenReturn(Optional.of(new ExchangeRate(1, new BigDecimal("3.0000"))));
        when(cryptoPriceClient.usdPrice("BTC")).thenReturn(Optional.of(new BigDecimal("65000")));

        Quote quote = rateService.quote("BTC");

        assertThat(quote).isInstanceOf(Quote.Crypto.class);
        assertThat(((Quote.Crypto) quote).priceInByn()).isEqualByComparingTo("195000");
    }

    @Test
    void showsCryptoInUsdWhenUsdRateIsUnavailable() {
        when(nbrbClient.find("BTC")).thenReturn(Optional.empty());
        when(nbrbClient.find("USD")).thenThrow(new ResourceAccessException("timeout"));
        when(cryptoPriceClient.usdPrice("BTC")).thenReturn(Optional.of(new BigDecimal("65000")));

        assertThat(rateService.quote("BTC")).isEqualTo(new Quote.Crypto("BTC", new BigDecimal("65000"), null));
    }

    @Test
    void reportsUnknownCurrency() {
        when(nbrbClient.find(anyString())).thenReturn(Optional.empty());
        when(cryptoPriceClient.usdPrice("XYZ")).thenReturn(Optional.empty());

        assertThat(rateService.quote("XYZ")).isEqualTo(new Quote.Unknown("XYZ"));
    }

    @Test
    void reportsOutageInsteadOfThrowing() {
        when(nbrbClient.find("USD")).thenThrow(new ResourceAccessException("timeout"));

        assertThat(rateService.quote("USD")).isEqualTo(new Quote.Unavailable("USD"));
    }
}