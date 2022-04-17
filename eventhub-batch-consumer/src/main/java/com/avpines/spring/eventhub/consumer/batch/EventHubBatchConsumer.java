package com.avpines.spring.eventhub.consumer.batch;

import com.avpines.spring.messaging.SimpleEvent;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
public class EventHubBatchConsumer {

  private final AtomicInteger counter = new AtomicInteger();

  @Bean
  LoggingMeterRegistry loggingMeterRegistry() {
    return new LoggingMeterRegistry();
  }

  @Bean
  public Consumer<Message<List<SimpleEvent>>> consume() {
    return message -> {
      counter.incrementAndGet();
      LOG.debug("Consumed '{}'", message.getPayload());
    };
  }

  @Scheduled(cron = "*/15 * * * * *")
  private void count() {
    LOG.info("Consumed {} messages", counter);
  }

}