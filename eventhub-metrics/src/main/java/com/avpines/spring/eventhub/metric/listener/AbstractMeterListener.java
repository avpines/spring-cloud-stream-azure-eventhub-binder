package com.avpines.spring.eventhub.metric.listener;

import com.avpines.spring.eventhub.metric.EventHubPropertyExtractor;
import com.avpines.spring.eventhub.metric.MeterListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

@Slf4j
public abstract class AbstractMeterListener implements MeterListener, MeterBinder {

  protected static final String NAMESPACE = "namespace";
  protected static final String DESTINATION = "destination";
  protected static final String PARTITION = "partition";
  protected static final String GROUP = "group";

  private EventHubPropertyExtractor eventHubExtractor;
  private boolean bounded = false;

  @Autowired
  public final void setEventHubExtractor(EventHubPropertyExtractor eventHubExtractor) {
    this.eventHubExtractor = eventHubExtractor;
  }

  @Override
  public void bindTo(@NotNull MeterRegistry registry) {
    bindToRegistry(registry);
    bounded = true;
    LOG.info("MeterListener Bounded to registry: '{}'", this.getClass().getSimpleName());
  }

  protected abstract void bindToRegistry(MeterRegistry registry);

  @Override
  public void capture(
      @NotNull MessageHeaders headers,
      @NotNull MessageChannel channel,
      boolean sent,
      @Nullable Exception ex) {
    if (bounded) {
      doCapture(
          EventInfo.builder()
              .headers(headers)
              .namespace(eventHubExtractor.extractNamespace(channel))
              .destination(eventHubExtractor.extractDestination(channel))
              .group(eventHubExtractor.extractGroup(channel))
              .partition(eventHubExtractor.extractPartition(headers))
              .sent(sent)
              .build()
      );
    }
  }

  protected abstract void doCapture(EventInfo event);

  @Value
  @Builder
  protected static class EventInfo {

    boolean sent;
    @NotNull String namespace;
    @NotNull String destination;
    @NotNull String group;
    @NotNull String partition;
    @NotNull MessageHeaders headers;

  }

}
