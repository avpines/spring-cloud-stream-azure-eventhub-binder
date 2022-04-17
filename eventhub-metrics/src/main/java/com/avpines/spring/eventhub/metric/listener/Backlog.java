package com.avpines.spring.eventhub.metric.listener;

import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.BATCH_CONVERTED_ENQUEUED_TIME;
import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.BATCH_CONVERTED_SEQUENCE_NUMBER;
import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.ENQUEUED_TIME;
import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.LAST_ENQUEUED_EVENT_PROPERTIES;
import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.SEQUENCE_NUMBER;

import com.avpines.dynamic.meters.gauge.SupplierDynamicGauge;
import com.azure.messaging.eventhubs.models.LastEnqueuedEventProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Backlog extends AbstractMeterListener {

  private static final String BACKLOG_SIZE = "eventhub.backlog.size";
  private static final String BACKLOG_DELAY = "eventhub.backlog.delay";

  private StatusHandle size;
  private StatusHandle delay;

  @Override
  protected void bindToRegistry(MeterRegistry registry) {
    this.size = new StatusHandle(registry, BACKLOG_SIZE);
    this.delay = new StatusHandle(registry, BACKLOG_DELAY);
  }

  @Override
  protected void doCapture(@NotNull EventInfo event) {
    computeSize(event);
    computeDelay(event);
  }

  private void computeDelay(@NotNull EventInfo event) {
    Instant curr = fromCurrent(event, Instant.class, ENQUEUED_TIME, BATCH_CONVERTED_ENQUEUED_TIME);
    Instant last = fromLastEnqueued(event, LastEnqueuedEventProperties::getEnqueuedTime);
    if (curr != null && last != null) {
      delay.forEvent(
              event.getNamespace(), event.getDestination(), event.getGroup(), event.getPartition())
          .set(
              Duration
                  .of(last.toEpochMilli() - curr.toEpochMilli(), ChronoUnit.MILLIS)
                  .toMinutes());
    }
  }

  private void computeSize(@NotNull EventInfo event) {
    Long curr = fromCurrent(event, Long.class, SEQUENCE_NUMBER, BATCH_CONVERTED_SEQUENCE_NUMBER);
    Long last = fromLastEnqueued(event, LastEnqueuedEventProperties::getSequenceNumber);
    if (curr != null && last != null) {
      size.forEvent(
              event.getNamespace(), event.getDestination(), event.getGroup(), event.getPartition())
          .set(last - curr);
    }
  }

  private <T> @Nullable T fromCurrent(
      @NotNull EventInfo event,
      @NotNull Class<T> type,
      @NotNull String... headers) {
    MessageHeaders mh = event.getHeaders();
    Object s = Arrays.stream(headers)
        .map(header -> Optional.ofNullable(mh.get(header, Object.class)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst().orElse(null);
    if (s != null) {
      if (ArrayList.class.isAssignableFrom(s.getClass())) {
        var asList = ((ArrayList<?>) s);
        return asList.isEmpty() ? null : type.cast(asList.get(asList.size() - 1));
      }
      return type.cast(s);
    }
    return null;
  }

  private <T> @Nullable T fromLastEnqueued(
      @NotNull EventInfo event,
      @NotNull Function<LastEnqueuedEventProperties, T> f) {
    return Optional.ofNullable(event.getHeaders()
            .get(LAST_ENQUEUED_EVENT_PROPERTIES, LastEnqueuedEventProperties.class))
        .map(f).orElse(null);
  }

  private static class StatusHandle {

    MeterRegistry registry;
    String name;
    ConcurrentMap<String, AtomicLong> handles;
    SupplierDynamicGauge gauge;

    public StatusHandle(@NotNull MeterRegistry registry, @NotNull String name) {
      this.name = name;
      this.registry = registry;
      this.handles = new ConcurrentHashMap<>();
      this.gauge = SupplierDynamicGauge.builder(registry, name)
          .tagKeys(NAMESPACE, DESTINATION, GROUP, PARTITION)
          .build();
    }

    private AtomicLong forEvent(
        @NotNull String namespace,
        @NotNull String destination,
        @NotNull String group,
        @NotNull String partition) {
      AtomicLong al = handles
          .computeIfAbsent(key(namespace, destination, group, partition), k -> new AtomicLong());
      gauge.getOrCreate(() -> al, namespace, destination, group, partition);
      return al;
    }

    private String key(String namespace, String destination, String group, String partition) {
      return String.format("%s::%s::%s::%s", namespace, destination, group, partition);
    }
  }

}
