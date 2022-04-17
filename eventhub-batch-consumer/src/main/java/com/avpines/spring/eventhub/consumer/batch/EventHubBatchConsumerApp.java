package com.avpines.spring.eventhub.consumer.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EventHubBatchConsumerApp {

    public static void main(String[] args) {
        SpringApplication.run(EventHubBatchConsumerApp.class, args);
    }

}
