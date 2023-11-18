package com.zagvladimir.configuration;


import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.zagvladimir.model.RabbitQueue.ANSWER;
import static org.zagvladimir.model.RabbitQueue.EXCHANGE;
import static org.zagvladimir.model.RabbitQueue.SCHEDULE;
import static org.zagvladimir.model.RabbitQueue.WEATHER;


@Configuration
public class RabbitConfiguration {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue weatherMessageQueue() {
        return new Queue(WEATHER);
    }

    @Bean
    public Queue answerMessageQueue() {
        return new Queue(ANSWER);
    }

    @Bean
    public Queue exchangeMessageQueue() {
        return new Queue(EXCHANGE);
    }

    @Bean
    public Queue scheduleMessageQueue() {
        return new Queue(SCHEDULE);
    }

}
