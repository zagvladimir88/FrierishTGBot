package com.zagvladimir.fraerish.exchange;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NbrbClient {

    private final RestClient restClient;

    public NbrbClient(RestClient.Builder restClientBuilder, NbrbProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    public ExchangeRate rate(String currencyCode) {
        return restClient.get()
                .uri("/exrates/rates/{code}?parammode=2", currencyCode)
                .retrieve()
                .body(ExchangeRate.class);
    }
}