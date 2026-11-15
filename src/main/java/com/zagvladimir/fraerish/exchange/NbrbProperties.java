package com.zagvladimir.fraerish.exchange;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.nbrb")
public record NbrbProperties(String baseUrl) {
}