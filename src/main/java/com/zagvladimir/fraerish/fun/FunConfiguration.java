package com.zagvladimir.fraerish.fun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

@Configuration
public class FunConfiguration {

    @Bean
    public RandomGenerator randomGenerator() {
        return new SecureRandom();
    }
}