package com.zagvladimir.fraerish.alerts;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.LocalTime;

@ConfigurationProperties("bot.weather-alerts")
public record WeatherAlertProperties(LocalTime activeFrom, LocalTime activeTo, Duration lookahead, double minChance) {

    public boolean isActiveAt(LocalTime time) {
        return !time.isBefore(activeFrom) && time.isBefore(activeTo);
    }
}