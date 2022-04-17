package com.avpines.spring.eventhub.transformer;

import com.avpines.spring.messaging.SimpleEvent;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Flux;

@Slf4j
@Configuration
@EnableScheduling
public class EventHubTransformerDlq {

  @Bean
  LoggingMeterRegistry loggingMeterRegistry() {
    return new LoggingMeterRegistry();
  }

  @Bean
  Function<Message<SimpleEvent>, Message<SimpleEvent>> transform() {
    return m -> {
      LOG.info("[transform()]-> transform() called");
      String data = m.getPayload().getData();
      if ("ERROR".equals(data)) {
        LOG.info("[transform()]-> throwing a new RuntimeException");
        throw new RuntimeException("Stuff happens");
      }
      return MessageBuilder
          .withPayload(m.getPayload().toBuilder().data(data + " (Transformed)").build())
          .copyHeaders(m.getHeaders())
          .build();
    };
  }

  // Used only for creating the binding, will not be used directly.
  @Bean
  Supplier<Flux<Message<?>>> dlq() {
    return () -> {
      LOG.warn("dlq supplier called (should happen only once)");
      return Flux.empty();
    };
  }

  /*
  @ServiceActivator(inputChannel = "{destination}.{group}.errors", outputChannel = "dlq-out-0")
  public Message<?> handleError(@Header("id") String id, @NotNull ErrorMessage message) {
    Message<?> original = message.getOriginalMessage();
    LOG.error("[handleError()]-> Handling exception via errorChannel, id='{}', message='{}'", id, original);
    return original;
  }
  */

  @Bean
  public IntegrationFlow routeToDlq(
      @Value("${spring.cloud.stream.bindings.transform-in-0.destination}") String destination,
      @Value("${spring.cloud.stream.bindings.transform-in-0.group}") String group) {
    String errorChannel = String.format("%s.%s.errors", destination, group);
    return IntegrationFlows
        .from(errorChannel)
        .transform(
            (MessagingException e) -> {
              Message<?> failedMessage = e.getFailedMessage();
              LOG.error("Handling exception via error channel '{}', id='{}', message='{}'",
                  errorChannel,
                  Optional.ofNullable(failedMessage).map(m -> m.getHeaders().get("id")).orElse("N/A"),
                  failedMessage);
              return failedMessage;
            }
        ).channel("dlq-out-0")
        .get();
  }

}
