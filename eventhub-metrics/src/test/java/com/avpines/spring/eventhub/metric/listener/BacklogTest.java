package com.avpines.spring.eventhub.metric.listener;

import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.BATCH_CONVERTED_ENQUEUED_TIME;
import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.BATCH_CONVERTED_SEQUENCE_NUMBER;
import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.ENQUEUED_TIME;
import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.LAST_ENQUEUED_EVENT_PROPERTIES;
import static com.azure.spring.messaging.eventhubs.support.EventHubsHeaders.SEQUENCE_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.avpines.spring.eventhub.metric.EventHubPropertyExtractor;
import com.avpines.spring.eventhub.metric.listener.AbstractMeterListener.EventInfo;
import com.azure.messaging.eventhubs.models.LastEnqueuedEventProperties;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

@ExtendWith(MockitoExtension.class)
class BacklogTest {

  private static final String BACKLOG_SIZE = "eventhub.backlog.size";
  private static final String BACKLOG_DELAY = "eventhub.backlog.delay";

  private SimpleMeterRegistry smr;

  @Mock
  private EventHubPropertyExtractor extractor;

  @BeforeEach
  void setup() {
    smr = new SimpleMeterRegistry();
  }

  @Test
  void singleRecord() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            SEQUENCE_NUMBER, 1200L,
            ENQUEUED_TIME, instant(12, 30, 0),
            LAST_ENQUEUED_EVENT_PROPERTIES, lastEnqueued(2100L, instant(13, 31, 20))
        )
    );
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.bindToRegistry(smr);
    backlog.doCapture(
        EventInfo.builder()
            .headers(mh).namespace("ns")
            .destination("dest")
            .group("group")
            .partition("part")
            .build());
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(2);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_SIZE)).hasSize(1)
        .extractingResultOf("value").containsExactly(900.0);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_DELAY)).hasSize(1)
        .extractingResultOf("value").containsExactly(61.0);
  }

  @Test
  void batch() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            BATCH_CONVERTED_SEQUENCE_NUMBER, asList(1200L, 1201L),
            BATCH_CONVERTED_ENQUEUED_TIME, asList(instant(12, 30, 0), instant(13, 45, 0)),
            LAST_ENQUEUED_EVENT_PROPERTIES, lastEnqueued(2000L, instant(13, 46, 20))
        )
    );
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.bindToRegistry(smr);
    backlog.doCapture(EventInfo.builder().headers(mh).namespace("ns").destination("dest")
        .group("group").partition("part").build());
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(2);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_SIZE)).hasSize(1)
        .extractingResultOf("value").containsExactly(799.0);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_DELAY)).hasSize(1)
        .extractingResultOf("value").containsExactly(1.0);
  }

  @Test
  void singleRecordNoCurrentSequence() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            ENQUEUED_TIME, instant(12, 30, 0),
            LAST_ENQUEUED_EVENT_PROPERTIES, lastEnqueued(2000L, instant(13, 31, 20))
        )
    );
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.bindToRegistry(smr);
    backlog.doCapture(EventInfo.builder().headers(mh).namespace("ns").destination("dest")
        .group("group").partition("part").build());
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(1);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_SIZE)).hasSize(0);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_DELAY)).hasSize(1)
        .extractingResultOf("value").containsExactly(61.0);
  }

  @Test
  void batchNoCurrentSequence() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            BATCH_CONVERTED_ENQUEUED_TIME, asList(instant(12, 30, 0), instant(13, 45, 0)),
            LAST_ENQUEUED_EVENT_PROPERTIES, lastEnqueued(2000L, instant(13, 46, 20))
        )
    );
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.bindToRegistry(smr);
    backlog.doCapture(EventInfo.builder().headers(mh).namespace("ns").destination("dest")
        .group("group").partition("part").build());
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(1);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_SIZE)).hasSize(0);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_DELAY)).hasSize(1)
        .extractingResultOf("value").containsExactly(1.0);
  }

  @Test
  void singleRecordNoCurrentDelay() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            SEQUENCE_NUMBER, 1500L,
            LAST_ENQUEUED_EVENT_PROPERTIES, lastEnqueued(2000L, instant(13, 31, 20))
        )
    );
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.bindToRegistry(smr);
    backlog.doCapture(
        EventInfo.builder()
            .headers(mh).namespace("ns")
            .destination("dest")
            .group("group")
            .partition("part")
            .build());
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(1);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_SIZE)).hasSize(1)
        .extractingResultOf("value").containsExactly(500.0);
  }

  @Test
  void batchNoCurrentDelay() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            BATCH_CONVERTED_SEQUENCE_NUMBER, asList(1200L, 1205L),
            LAST_ENQUEUED_EVENT_PROPERTIES, lastEnqueued(2000L, instant(13, 50, 20))
        )
    );
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.bindToRegistry(smr);
    backlog.doCapture(EventInfo.builder().headers(mh).namespace("ns").destination("dest")
        .group("group").partition("part").build());
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(1);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_SIZE)).hasSize(1)
        .extractingResultOf("value").containsExactly(795.0);
  }

  @Test
  void singleRecordNoLastSequence() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            SEQUENCE_NUMBER, 1000L,
            ENQUEUED_TIME, instant(15, 30, 0)
        )
    );
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.bindToRegistry(smr);
    backlog.doCapture(EventInfo.builder().headers(mh).namespace("ns").destination("dest")
        .group("group").partition("part").build());
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(0);
  }

  @Test
  void batchNoLastSequence() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            BATCH_CONVERTED_SEQUENCE_NUMBER, asList(1200L, 1300L),
            BATCH_CONVERTED_ENQUEUED_TIME, asList(instant(12, 30, 0), instant(13, 45, 0))
        )
    );
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.bindToRegistry(smr);
    backlog.doCapture(EventInfo.builder().headers(mh).namespace("ns").destination("dest")
        .group("group").partition("part").build());
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(0);
  }

  @Test
  void captureCalledBeforeBound() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            SEQUENCE_NUMBER, 1200L,
            ENQUEUED_TIME, instant(12, 30, 0),
            LAST_ENQUEUED_EVENT_PROPERTIES, lastEnqueued(2000L, instant(13, 31, 20))
        )
    );
    MessageChannel mockChannel = mock(MessageChannel.class);
    lenient().when(extractor.extractNamespace(mockChannel)).thenReturn("ns");
    lenient().when(extractor.extractDestination(mockChannel)).thenReturn("dest");
    lenient().when(extractor.extractGroup(mockChannel)).thenReturn("group");
    lenient().when(extractor.extractPartition(mh)).thenReturn("part");
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.capture(mh, mockChannel, true, null);
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(0);
  }

  @Test
  void captureCalledAfterBound() {
    MessageHeaders mh = new MessageHeaders(
        Map.of(
            SEQUENCE_NUMBER, 1200L,
            ENQUEUED_TIME, instant(12, 30, 0),
            LAST_ENQUEUED_EVENT_PROPERTIES, lastEnqueued(2000L, instant(13, 31, 20))
        )
    );
    MessageChannel mockChannel = mock(MessageChannel.class);
    when(extractor.extractNamespace(mockChannel)).thenReturn("ns");
    when(extractor.extractDestination(mockChannel)).thenReturn("dest");
    when(extractor.extractGroup(mockChannel)).thenReturn("group");
    when(extractor.extractPartition(mh)).thenReturn("part");
    Backlog backlog = new Backlog();
    backlog.setEventHubExtractor(extractor);
    backlog.bindTo(smr);
    backlog.capture(mh, mockChannel, true, null);
    List<Meter> meters = smr.getMeters();
    assertThat(meters).hasSize(2);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_SIZE)).hasSize(1)
        .extractingResultOf("value").containsExactly(800.0);
    assertThat(meters).filteredOn(m -> m.getId().getName().equals(BACKLOG_DELAY)).hasSize(1)
        .extractingResultOf("value").containsExactly(61.0);
  }

  private Instant instant(int hour, int minute, int second) {
    return LocalDateTime.of(2022, 1, 1, hour, minute, second)
        .toInstant(ZoneOffset.UTC);
  }

  private @NotNull LastEnqueuedEventProperties lastEnqueued(Long sequence, Instant enqueueTime) {
    return new LastEnqueuedEventProperties(sequence,
        2000L,
        enqueueTime,
        enqueueTime.plus(4, ChronoUnit.SECONDS)
    );
  }

  @SafeVarargs
  private <T> @NotNull List<T> asList(@NotNull T... values) {
    // Return a mutable list, as opposed to List#of and Arrays#asList
    return new ArrayList<>(Arrays.asList(values));
  }

}