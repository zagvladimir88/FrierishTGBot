package com.zagvladimir.fraerish.weather;

public sealed interface CityWeather {

    String text();

    record Found(String name, String text) implements CityWeather {
    }

    record NotFound(String query) implements CityWeather {

        @Override
        public String text() {
            return query + " — не нашёл такой город";
        }
    }

    record Unavailable(String query) implements CityWeather {

        @Override
        public String text() {
            return query + " — погода сейчас недоступна";
        }
    }
}