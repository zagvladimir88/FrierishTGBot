package com.zagvladimir.fraerish.exchange;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RateClientsTest {

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final NbrbClient nbrbClient = new NbrbClient(restClientBuilder, new NbrbProperties("https://nbrb.test"));
    private final BinancePriceClient binanceClient = new BinancePriceClient(restClientBuilder, new BinanceProperties("https://binance.test"));

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void readsOfficialRate() {
        server.expect(requestTo("https://nbrb.test/exrates/rates/USD?parammode=2")).andRespond(withSuccess("""
                {"Cur_ID": 431, "Date": "2026-11-15T00:00:00", "Cur_Abbreviation": "USD",
                 "Cur_Scale": 1, "Cur_Name": "Доллар США", "Cur_OfficialRate": 3.0026}""", MediaType.APPLICATION_JSON));

        assertThat(nbrbClient.find("USD")).contains(new ExchangeRate(1, new BigDecimal("3.0026")));
    }

    @Test
    void treatsMissingCurrencyAsEmpty() {
        server.expect(requestTo("https://nbrb.test/exrates/rates/BTC?parammode=2")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(nbrbClient.find("BTC")).isEmpty();
    }

    @Test
    void propagatesNbrbOutage() {
        server.expect(requestTo("https://nbrb.test/exrates/rates/USD?parammode=2")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> nbrbClient.find("USD")).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void readsCryptoPriceInUsdt() {
        server.expect(requestTo("https://binance.test/api/v3/ticker/price?symbol=BTCUSDT"))
                .andRespond(withSuccess("{\"symbol\":\"BTCUSDT\",\"price\":\"65080.01000000\"}", MediaType.APPLICATION_JSON));

        assertThat(binanceClient.usdPrice("BTC")).contains(new BigDecimal("65080.01000000"));
    }

    @Test
    void treatsInvalidSymbolAsEmpty() {
        server.expect(requestTo("https://binance.test/api/v3/ticker/price?symbol=XYZUSDT"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":-1121,\"msg\":\"Invalid symbol.\"}"));

        assertThat(binanceClient.usdPrice("XYZ")).isEmpty();
    }
}