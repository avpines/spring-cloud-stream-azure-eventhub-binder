package com.avpines.spring.eventhub.consumer;

import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EventHubConsumerApp {

    public static void main(String[] args) {
        SpringApplication.run(EventHubConsumerApp.class, args);
    }

    @Bean
    LoggingMeterRegistry loggingMeterRegistry() {
        return new LoggingMeterRegistry();
    }

}
