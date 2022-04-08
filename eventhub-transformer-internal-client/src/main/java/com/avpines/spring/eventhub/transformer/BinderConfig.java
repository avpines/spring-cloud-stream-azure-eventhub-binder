package com.avpines.spring.eventhub.transformer;

import com.azure.core.amqp.AmqpRetryMode;
import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.spring.cloud.stream.binder.eventhubs.EventHubsMessageChannelBinder;
import com.azure.spring.cloud.stream.binder.eventhubs.config.EventHubsProcessorFactoryCustomizer;
import com.azure.spring.cloud.stream.binder.eventhubs.config.EventHubsProducerFactoryCustomizer;
import com.azure.spring.messaging.eventhubs.core.DefaultEventHubsNamespaceProcessorFactory;
import com.azure.spring.messaging.eventhubs.core.DefaultEventHubsNamespaceProducerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.BinderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class BinderConfig {

  @Bean
  @SuppressWarnings("rawtypes")
  public BinderCustomizer binderCustomizer(
      EventHubsProducerFactoryCustomizer producerFactoryCustomizer,
      EventHubsProcessorFactoryCustomizer processorFactoryCustomizer) {
    return (binder, binderName) -> {
      if (EventHubsMessageChannelBinder.class.isAssignableFrom(binder.getClass())) {
        var b = (EventHubsMessageChannelBinder) ((Binder) binder);
        b.addProducerFactoryCustomizer(producerFactoryCustomizer);
        b.addProcessorFactoryCustomizer(processorFactoryCustomizer);
      }
    };
  }

  @Bean
  EventHubsProducerFactoryCustomizer producerFactoryCustomizer() {
    return factory -> {
      if (DefaultEventHubsNamespaceProducerFactory.class.isAssignableFrom(factory.getClass())) {
        // AzureServiceClientBuilderCustomizer<EventHubClientBuilder> in lambda form
        ((DefaultEventHubsNamespaceProducerFactory) factory).addBuilderCustomizer(
            builder -> {
              LOG.info("Customizing EventHubsProducerFactoryCustomizer");
              builder.retryOptions(new AmqpRetryOptions().setMode(AmqpRetryMode.FIXED));
            }
        );
      }
    };
  }

  @Bean
  EventHubsProcessorFactoryCustomizer processorFactoryCustomizer() {
    return factory -> {
      if (DefaultEventHubsNamespaceProcessorFactory.class.isAssignableFrom(factory.getClass())) {
        ((DefaultEventHubsNamespaceProcessorFactory) factory).addBuilderCustomizer(
            builder -> {
              LOG.info("Customizing EventHubsProcessorFactory");
              builder.retryOptions(new AmqpRetryOptions().setMode(AmqpRetryMode.EXPONENTIAL));
            }
        );
      }
    };
  }

}
