package com.zagvladimir.fraerish.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRate(
        @JsonProperty("Cur_Scale") int scale,
        @JsonProperty("Cur_Name") String currencyName,
        @JsonProperty("Cur_OfficialRate") double officialRate) {
}