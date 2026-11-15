package com.zagvladimir.fraerish.exchange;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.digest.DigestProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExchangeHandlerTest {

    private static final DigestProperties PROPERTIES = new DigestProperties(ZoneId.of("Europe/Minsk"), "по Минску", Duration.ofMinutes(30), 5, 5);

    private final RateService rateService = mock(RateService.class);
    private final ExchangeHandler handler = new ExchangeHandler(rateService, PROPERTIES);

    @Test
    void showsEveryRequestedCurrency() {
        when(rateService.quote("USD")).thenReturn(new Quote.Fiat("USD", 1, new BigDecimal("3.0026")));
        when(rateService.quote("BTC")).thenReturn(new Quote.Crypto("BTC", new BigDecimal("65080"), null));
        when(rateService.quote("XYZ")).thenReturn(new Quote.Unknown("XYZ"));

        BotReply reply = handler.handle(new CommandRequest(1, "usd,btc xyz"));

        assertThat(reply).isEqualTo(BotReply.text("USD — 3.00 BYN\nBTC — 65 080 $\nXYZ — не знаю такую валюту"));
    }

    @Test
    void explainsUsageWithoutCurrencies() {
        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(ExchangeHandler.USAGE));
        verifyNoInteractions(rateService);
    }
}