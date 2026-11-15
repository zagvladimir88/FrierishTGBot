package com.zagvladimir.fraerish.exchange;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class NbrbClient {

    private final RestClient restClient;

    public NbrbClient(RestClient.Builder restClientBuilder, NbrbProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    public Optional<ExchangeRate> find(String currencyCode) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/exrates/rates/{code}?parammode=2", currencyCode)
                    .retrieve()
                    .body(ExchangeRate.class));
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.BadRequest e) {
            return Optional.empty();
        }
    }
}