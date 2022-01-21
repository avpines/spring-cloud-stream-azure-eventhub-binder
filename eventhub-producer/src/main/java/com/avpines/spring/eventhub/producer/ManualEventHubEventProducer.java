package com.avpines.spring.eventhub.producer;

import com.avpines.spring.messaging.SimpleEvent;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * This event producer will allow sending events via Controller endpoints
 */
@Slf4j
@Configuration
@EnableScheduling
@Profile("manual")
public class ManualEventHubEventProducer {

  @Bean
  public Sinks.Many<Message<SimpleEvent>> sinkMany() {
    return Sinks.many().unicast().onBackpressureBuffer();
  }

  @Bean
  Supplier<Flux<Message<SimpleEvent>>> produce(
      Sinks.Many<Message<SimpleEvent>> sink) {
    return () -> sink.asFlux()
        .doOnNext(m -> LOG.info("Sent message '{}'", m))
        .doOnError(t -> LOG.warn("Encountered an error ({})", t.getMessage()));
  }

}
