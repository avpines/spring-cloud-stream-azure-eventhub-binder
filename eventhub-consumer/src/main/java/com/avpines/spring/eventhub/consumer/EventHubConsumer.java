package com.avpines.spring.eventhub.consumer;

import java.util.Random;
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
public class EventHubConsumer {

  private static final Random RND = new Random();

  private final AtomicInteger counter = new AtomicInteger();

  @Bean
  public Consumer<Message<String>> consume() {
    return message -> {
      counter.incrementAndGet();
      simulateHardWork(10, 300);
      LOG.info("Consumed '{}'", message.getPayload());
    };
  }

  @SuppressWarnings("SameParameterValue")
  private void simulateHardWork(int minDelay, int maxDelay) {
    try {
      Thread.sleep(RND.nextInt(maxDelay - minDelay) + minDelay);
    } catch (InterruptedException e) {
      LOG.info("Thread interrupted", e);
    }
  }

  @Scheduled(cron = "*/15 * * * * *")
  private void count() {
    LOG.info("Consumed {} messages", counter);
  }

}