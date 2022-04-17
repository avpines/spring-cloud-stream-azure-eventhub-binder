package com.avpines.spring.eventhub.metric.listener;

import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.BATCH_CONVERTED_SEQUENCE_NUMBER;
import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.SEQUENCE_NUMBER;

import com.avpines.dynamic.meters.counter.DynamicCounter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Count extends AbstractMeterListener {

  private static final String EVENTS_COUNT = "eventhub.events.count";

  private DynamicCounter counter;

  @Override
  protected void bindToRegistry(MeterRegistry registry) {
    counter = DynamicCounter.builder(registry, EVENTS_COUNT)
        .tagKeys(NAMESPACE, DESTINATION, GROUP, PARTITION)
        .build();
  }

  @Override
  protected void doCapture(@NotNull EventInfo event) {
    MessageHeaders mh = event.getHeaders();
    Optional.ofNullable(mh.get(SEQUENCE_NUMBER, Object.class))
        .or(() -> Optional.ofNullable(mh.get(BATCH_CONVERTED_SEQUENCE_NUMBER, Object.class)))
        .ifPresent(s -> counter.getOrCreate(event.getNamespace(), event.getDestination(),
                event.getGroup(), event.getPartition())
            .increment(ArrayList.class.isAssignableFrom(s.getClass())
                ? ((ArrayList<?>) s).size() : 1.0)
        );
  }

}
