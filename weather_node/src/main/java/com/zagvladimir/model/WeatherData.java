package com.zagvladimir.model;

import java.util.List;

public record WeatherData(
        Coord coord,
        List<Weather> weather,
        String base,
        Main main,
        int visibility,
        Wind wind,
        Clouds clouds,
        long dt,
        Sys sys,
        int timezone,
        int id,
        String name,
        int cod
) {}