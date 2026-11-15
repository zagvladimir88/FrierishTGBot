package com.zagvladimir.fraerish.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class BinancePriceClient implements CryptoPriceClient {

    private static final String QUOTE_ASSET = "USDT";

    private final RestClient restClient;

    public BinancePriceClient(RestClient.Builder restClientBuilder, BinanceProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Optional<BigDecimal> usdPrice(String symbol) {
        try {
            TickerPrice ticker = restClient.get()
                    .uri("/api/v3/ticker/price?symbol={pair}", symbol + QUOTE_ASSET)
                    .retrieve()
                    .body(TickerPrice.class);
            return Optional.ofNullable(ticker).map(TickerPrice::price);
        } catch (HttpClientErrorException.BadRequest e) {
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TickerPrice(String symbol, BigDecimal price) {
    }
}