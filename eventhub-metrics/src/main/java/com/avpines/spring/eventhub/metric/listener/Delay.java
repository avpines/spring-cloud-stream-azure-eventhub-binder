package com.avpines.spring.eventhub.metric.listener;

import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.LAST_ENQUEUED_EVENT_PROPERTIES;

import com.avpines.dynamic.meters.counter.DynamicCounter;
import com.avpines.dynamic.meters.timer.DynamicTimer;
import com.azure.messaging.eventhubs.models.LastEnqueuedEventProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Delay extends AbstractMeterListener {

  private static final String EVENTS_COUNT = "eventhub.delay";

  private DynamicTimer timer;

  @Override
  protected void bindToRegistry(MeterRegistry registry) {
    timer = DynamicTimer.builder(registry, EVENTS_COUNT)
        .tagKeys(NAMESPACE, DESTINATION, GROUP, PARTITION)
        .build();
  }

  @Override
  protected void doCapture(@NotNull EventInfo event) {
    long now = System.currentTimeMillis();
    Optional.ofNullable(event.getHeaders()
            .get(LAST_ENQUEUED_EVENT_PROPERTIES, LastEnqueuedEventProperties.class))
        .map(LastEnqueuedEventProperties::getEnqueuedTime)
        .ifPresent(last -> timer.getOrCreate(event.getNamespace(), event.getDestination(),
                event.getGroup(), event.getPartition())
            .record(now - last.toEpochMilli(), TimeUnit.MILLISECONDS));
  }

}
