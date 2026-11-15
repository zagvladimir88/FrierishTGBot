package com.zagvladimir.fraerish.telegram;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramConfiguration {

    @Bean
    public TelegramClient telegramClient(BotProperties properties) {
        return new OkHttpTelegramClient(properties.token());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "bot.polling-enabled", havingValue = "true", matchIfMissing = true)
    public TelegramBotsLongPollingApplication telegramBotsApplication(BotProperties properties,
                                                                      UpdateDispatcher dispatcher) throws TelegramApiException {
        TelegramBotsLongPollingApplication application = new TelegramBotsLongPollingApplication();
        application.registerBot(properties.token(), dispatcher);
        return application;
    }
}