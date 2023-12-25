package com.zagvladimir.configuration;


import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.zagvladimir.model.RabbitQueue.ANSWER_PHOTO_QUEUE;
import static org.zagvladimir.model.RabbitQueue.ANSWER_TEXT_QUEUE;
import static org.zagvladimir.model.RabbitQueue.EXCHANGE_QUEUE;
import static org.zagvladimir.model.RabbitQueue.GOOGLE_SEARCH_QUEUE;
import static org.zagvladimir.model.RabbitQueue.SCHEDULE_QUEUE;
import static org.zagvladimir.model.RabbitQueue.TEXT_CONVERSION_QUEUE;
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
    public Queue answerTextMessageQueue() {
        return new Queue(ANSWER_TEXT_QUEUE);
    }

    @Bean
    public Queue answerPhotoMessageQueue() {return new Queue(ANSWER_PHOTO_QUEUE);}

    @Bean
    public Queue exchangeMessageQueue() {
        return new Queue(EXCHANGE_QUEUE);
    }

    @Bean
    public Queue scheduleMessageQueue() {
        return new Queue(SCHEDULE_QUEUE);
    }

    @Bean
    public Queue googleSearchMessageQueue() {
        return new Queue(GOOGLE_SEARCH_QUEUE);
    }

    @Bean
    public Queue textConversationMessageQueue() {return new Queue(TEXT_CONVERSION_QUEUE);}
}
