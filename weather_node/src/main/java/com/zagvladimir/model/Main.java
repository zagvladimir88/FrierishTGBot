package com.zagvladimir.model;

public record Main(
        double temp,
        double feels_like,
        double temp_min,
        double temp_max,
        int pressure,
        int humidity
) {}