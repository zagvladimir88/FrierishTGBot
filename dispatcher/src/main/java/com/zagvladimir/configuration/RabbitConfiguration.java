package com.zagvladimir.configuration;


import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.zagvladimir.model.RabbitQueue.ANSWER_QUEUE;
import static org.zagvladimir.model.RabbitQueue.EXCHANGE_QUEUE;
import static org.zagvladimir.model.RabbitQueue.SCHEDULE_QUEUE;
import static org.zagvladimir.model.RabbitQueue.WEATHER_QUEUE;


@Configuration
public class RabbitConfiguration {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue weatherMessageQueue() {
        return new Queue(WEATHER_QUEUE);
    }

    @Bean
    public Queue answerMessageQueue() {
        return new Queue(ANSWER_QUEUE);
    }

    @Bean
    public Queue exchangeMessageQueue() {
        return new Queue(EXCHANGE_QUEUE);
    }

    @Bean
    public Queue scheduleMessageQueue() {
        return new Queue(SCHEDULE_QUEUE);
    }

}
