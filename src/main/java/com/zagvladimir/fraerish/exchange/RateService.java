package com.zagvladimir.fraerish.exchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateService {

    private static final String USD = "USD";

    private final NbrbClient nbrbClient;
    private final CryptoPriceClient cryptoPriceClient;

    public Quote quote(String code) {
        try {
            return nbrbClient.find(code)
                    .<Quote>map(rate -> new Quote.Fiat(code, rate.scale(), rate.officialRate()))
                    .orElseGet(() -> crypto(code));
        } catch (RestClientException e) {
            log.warn("Rate for {} is unavailable: {}", code, e.getMessage());
            return new Quote.Unavailable(code);
        }
    }

    private Quote crypto(String code) {
        return cryptoPriceClient.usdPrice(code)
                .<Quote>map(usd -> new Quote.Crypto(code, usd, toByn(usd).orElse(null)))
                .orElseGet(() -> new Quote.Unknown(code));
    }

    private Optional<BigDecimal> toByn(BigDecimal usd) {
        try {
            return nbrbClient.find(USD).map(rate -> usd.multiply(rate.officialRate())
                    .divide(BigDecimal.valueOf(rate.scale()), MathContext.DECIMAL64));
        } catch (RestClientException e) {
            log.warn("USD rate is unavailable, crypto price is shown in USD only: {}", e.getMessage());
            return Optional.empty();
        }
    }
}