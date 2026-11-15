package com.zagvladimir.fraerish.exchange;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ExchangeHandler implements CommandHandler {

    static final String USAGE = "Укажите валюту: /exchange USD";
    static final String CURRENCY_NOT_FOUND = "Incorrect currency id";

    private final NbrbClient nbrbClient;

    @Override
    public String command() {
        return "/exchange";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        return request.hasArgs() ? rate(request.firstArg()) : BotReply.text(USAGE);
    }

    public BotReply rate(String currencyCode) {
        try {
            ExchangeRate rate = nbrbClient.rate(currencyCode.toUpperCase(Locale.ROOT));
            return BotReply.text("Курс %s %s - %s BYN".formatted(rate.scale(), rate.currencyName(), rate.officialRate()));
        } catch (HttpClientErrorException.NotFound e) {
            return BotReply.text(CURRENCY_NOT_FOUND);
        }
    }
}