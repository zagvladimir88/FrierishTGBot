package com.zagvladimir.configuration;

import com.zagvladimir.bot.FraerishCompanionBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class FraerishCompanionBotConfiguration {

    @Bean
    public TelegramBotsApi telegramBotsApi(FraerishCompanionBot fraerishCompanionBot) throws TelegramApiException {
        var api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(fraerishCompanionBot);
        return api;
    }
}
