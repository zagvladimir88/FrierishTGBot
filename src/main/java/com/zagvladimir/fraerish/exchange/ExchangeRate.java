package com.zagvladimir.fraerish.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRate(
        @JsonProperty("Cur_Scale") int scale,
        @JsonProperty("Cur_OfficialRate") BigDecimal officialRate) {
}