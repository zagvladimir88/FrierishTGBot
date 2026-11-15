package com.zagvladimir.fraerish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class FraerishBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraerishBotApplication.class, args);
    }
}