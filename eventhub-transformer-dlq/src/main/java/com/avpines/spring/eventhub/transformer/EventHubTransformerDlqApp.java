package com.avpines.spring.eventhub.transformer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import java.time.Duration;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EventHubTransformerDlqApp {

    public static void main(String[] args) {
        SpringApplication.run(EventHubTransformerDlqApp.class, args);
    }

    @Bean
    LoggingMeterRegistry loggingMeterRegistry() {
        return new LoggingMeterRegistry(
            new LoggingRegistryConfig() {
                @Override
                public String get(@NotNull String key) {
                    return null;
                }

                @Override
                public @NotNull Duration step() {
                    return Duration.ofHours(1);
                }
            },
            Clock.SYSTEM);
    }

}
