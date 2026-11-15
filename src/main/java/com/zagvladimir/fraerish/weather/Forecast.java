package com.zagvladimir.fraerish.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Forecast(List<Slot> list, City city) {

    public List<Slot> slots() {
        return list == null ? List.of() : list;
    }

    public int timezoneOffsetSeconds() {
        return city == null ? 0 : city.timezone();
    }

    public String cityName() {
        return city == null || city.name() == null ? "" : city.name();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Slot(long dt, CurrentWeather.Temperature main, List<CurrentWeather.Condition> weather, double pop) {

        public CurrentWeather.Condition condition() {
            return weather == null || weather.isEmpty() ? CurrentWeather.Condition.UNKNOWN : weather.getFirst();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record City(String name, int timezone) {
    }
}