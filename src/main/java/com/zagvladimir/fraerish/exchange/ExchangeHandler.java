package com.zagvladimir.fraerish.exchange;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.command.ListInput;
import com.zagvladimir.fraerish.digest.DigestProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExchangeHandler implements CommandHandler {

    static final String USAGE = "Укажите валюту или несколько через запятую: /exchange USD, EUR, BTC";

    private final RateService rateService;
    private final DigestProperties digestProperties;

    @Override
    public String command() {
        return "/exchange";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        List<String> codes = ListInput.currencies(request.args());
        if (codes.isEmpty()) {
            return BotReply.text(USAGE);
        }
        return BotReply.text(codes.stream()
                .limit(digestProperties.maxCurrencies())
                .map(rateService::quote)
                .map(QuoteFormatter::format)
                .collect(Collectors.joining("\n")));
    }
}