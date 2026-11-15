package com.zagvladimir.fraerish.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentWeather(String name, List<Condition> weather, Temperature main) {

    public Condition condition() {
        return weather == null || weather.isEmpty() ? Condition.UNKNOWN : weather.getFirst();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Condition(String main, String description) {

        static final Condition UNKNOWN = new Condition("", "");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Temperature(double temp, @JsonProperty("feels_like") double feelsLike) {
    }
}