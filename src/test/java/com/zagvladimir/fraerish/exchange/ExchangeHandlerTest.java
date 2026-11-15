package com.zagvladimir.fraerish.exchange;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExchangeHandlerTest {

    private static final String USD_URL = "https://nbrb.test/exrates/rates/USD?parammode=2";

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final ExchangeHandler handler = new ExchangeHandler(
            new NbrbClient(restClientBuilder, new NbrbProperties("https://nbrb.test")));

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void formatsOfficialRate() {
        server.expect(requestTo(USD_URL)).andRespond(withSuccess("""
                {"Cur_ID": 431, "Date": "2026-11-15T00:00:00", "Cur_Abbreviation": "USD",
                 "Cur_Scale": 1, "Cur_Name": "Доллар США", "Cur_OfficialRate": 3.0026}""", MediaType.APPLICATION_JSON));

        assertThat(handler.handle(new CommandRequest(1, "USD"))).isEqualTo(BotReply.text("Курс 1 Доллар США - 3.0026 BYN"));
    }

    @Test
    void usesFirstArgumentInUpperCase() {
        server.expect(requestTo(USD_URL)).andRespond(withSuccess("""
                {"Cur_Scale": 1, "Cur_Name": "Доллар США", "Cur_OfficialRate": 3.0026}""", MediaType.APPLICATION_JSON));

        assertThat(handler.handle(new CommandRequest(1, "usd eur"))).isEqualTo(BotReply.text("Курс 1 Доллар США - 3.0026 BYN"));
    }

    @Test
    void reportsUnknownCurrency() {
        server.expect(requestTo("https://nbrb.test/exrates/rates/XYZ?parammode=2")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(handler.handle(new CommandRequest(1, "XYZ"))).isEqualTo(BotReply.text(ExchangeHandler.CURRENCY_NOT_FOUND));
    }

    @Test
    void propagatesServiceFailures() {
        server.expect(requestTo(USD_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> handler.rate("USD")).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void asksForCurrencyWhenArgumentsAreMissing() {
        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(ExchangeHandler.USAGE));
    }
}