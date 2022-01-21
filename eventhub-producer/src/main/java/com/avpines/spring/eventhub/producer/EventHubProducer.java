package com.avpines.spring.eventhub.producer;

import com.avpines.spring.messaging.SimpleEvent;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Will produce a message every ${spring.cloud.stream.poller.fixed-delay} milliseconds, defaults to
 * 1 second.
 */
@Slf4j
@Configuration
@EnableScheduling
@Profile("!manual")
public class EventHubProducer {

  private static final Random RND = new Random();

  private static final String[] QUOTES = {
      "Before software can be reusable it first has to be usable",
      "One man’s crappy software is another man’s full-time job",
      "Deleted code is debugged code",
      "If at first you don’t succeed; call it version 1.0",
      "It’s not a bug – it’s an undocumented feature",
      "There are only two industries that refer to their customers as 'users'",
      "Never trust a computer you can’t throw out a window",
      "The function of good software is to make the complex appear to be simple",
      "If Java had true garbage collection, most programs would delete themselves upon execution",
      "To err is human, but to really foul things up you need a computer"
  };

  @Bean
  Supplier<Message<SimpleEvent>> produce() {
    return () -> MessageBuilder
        .withPayload(SimpleEvent.builder()
            .id(UUID.randomUUID().toString())
            .data(QUOTES[RND.nextInt(QUOTES.length)])
            .build())
        .build();
  }

}
