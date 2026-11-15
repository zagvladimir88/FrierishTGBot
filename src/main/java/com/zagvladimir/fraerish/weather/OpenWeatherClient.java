package com.zagvladimir.fraerish.weather;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenWeatherClient {

    private final RestClient restClient;
    private final String apiKey;

    public OpenWeatherClient(RestClient.Builder restClientBuilder, OpenWeatherProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.apiKey = properties.apiKey();
    }

    public CurrentWeather currentWeather(String city) {
        return restClient.get()
                .uri("/data/2.5/weather?q={city}&appid={apiKey}&units=metric&lang=ru", city, apiKey)
                .retrieve()
                .body(CurrentWeather.class);
    }

    public Forecast forecast(String city, int slots) {
        return restClient.get()
                .uri("/data/2.5/forecast?q={city}&appid={apiKey}&units=metric&lang=ru&cnt={slots}", city, apiKey, slots)
                .retrieve()
                .body(Forecast.class);
    }
}