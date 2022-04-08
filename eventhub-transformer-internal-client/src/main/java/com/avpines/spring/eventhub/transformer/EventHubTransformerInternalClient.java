package com.avpines.spring.eventhub.transformer;

import com.avpines.spring.messaging.SimpleEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
public class EventHubTransformerInternalClient {

  private final AtomicInteger counter = new AtomicInteger();

  @Bean
  Function<Message<SimpleEvent>, Message<SimpleEvent>> transform() {
    return m -> {
      LOG.info("[transform()]-> transform() called");
      counter.incrementAndGet();
      return MessageBuilder
          .withPayload(m.getPayload().toBuilder()
              .data(m.getPayload().getData() + " (Transformed)")
              .build())
          .copyHeaders(m.getHeaders())
          .build();
    };
  }

  @Scheduled(cron = "*/15 * * * * *")
  private void count() {
    LOG.info("Transformed {} messages", counter);
  }

}
