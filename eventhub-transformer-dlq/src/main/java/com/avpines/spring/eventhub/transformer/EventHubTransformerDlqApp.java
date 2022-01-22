package com.avpines.spring.eventhub.transformer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EventHubTransformerDlqApp {

    public static void main(String[] args) {
        SpringApplication.run(EventHubTransformerDlqApp.class, args);
    }

}
