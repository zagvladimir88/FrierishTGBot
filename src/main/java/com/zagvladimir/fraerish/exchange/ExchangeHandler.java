package com.zagvladimir.fraerish.exchange;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.command.ListInput;
import com.zagvladimir.fraerish.digest.ChatRepository;
import com.zagvladimir.fraerish.digest.ChatSettings;
import com.zagvladimir.fraerish.digest.DigestProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Order(3)
@Component
@RequiredArgsConstructor
public class ExchangeHandler implements CommandHandler {

    static final String USAGE = """
            Укажите валюту или несколько через запятую: /e USD, EUR, BTC
            Пересчитать сумму: /e 100 usd eur, /e 0.5 btc, /e 50$""";

    private final RateService rateService;
    private final CurrencyConverter currencyConverter;
    private final ChatRepository chatRepository;
    private final DigestProperties digestProperties;

    @Override
    public String command() {
        return "/e";
    }

    @Override
    public List<String> aliases() {
        return List.of("/exchange");
    }

    @Override
    public String description() {
        return "Курсы и пересчёт: /e USD, EUR или /e 100 usd eur";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        Optional<Conversion> conversion = Conversion.parse(request.args());
        if (conversion.isPresent()) {
            return BotReply.text(currencyConverter.convert(conversion.get()));
        }
        List<String> codes = request.hasArgs() ? ListInput.currencies(request.args()) : savedCurrencies(request.chatId());
        if (codes.isEmpty()) {
            return BotReply.text(USAGE);
        }
        return BotReply.text(codes.stream()
                .limit(digestProperties.maxCurrencies())
                .map(rateService::quote)
                .map(QuoteFormatter::format)
                .collect(Collectors.joining("\n")));
    }

    private List<String> savedCurrencies(long chatId) {
        return chatRepository.find(chatId).map(ChatSettings::currencies).orElse(List.of());
    }
}