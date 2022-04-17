package com.avpines.spring.eventhub.metric;

import static com.azure.spring.messaging.AzureHeaders.PARTITION_ID;
import static com.azure.spring.messaging.AzureHeaders.RAW_PARTITION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.config.BinderProperties;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

@ExtendWith(MockitoExtension.class)
class EventHubPropertyExtractorTest {

  private static final String UNKNOWN = "UNKNOWN";

  @Mock
  BindingServiceProperties bsp;

  EventHubPropertyExtractor extract;

  @BeforeEach
  void resetExtractor() {
    extract = new EventHubPropertyExtractor();
    extract.setBindingServiceProperties(bsp);
  }

  @Test
  void bindingToAbstractChannel() {
    var b = new BindingProperties();
    when(bsp.getBindingProperties("my-channel")).thenReturn(b);
    assertThat(extract.bindingOf(new NoOp("my-channel"))).isPresent().contains(b);
  }

  @Test
  void bindingOfNotAbstractMessageChannel() {
    var b = new BindingProperties();
    when(bsp.getBindingProperties("my-channel")).thenReturn(b);
    var mockChannel = mock(MessageChannel.class);
    when(mockChannel.toString()).thenReturn(String.format("bean '%s'", "my-channel"));
    assertThat(extract.bindingOf(mockChannel)).isPresent().contains(b);
  }

  @Test
  void bindingOfNotRecognizable() {
    var mockChannel = mock(MessageChannel.class);
    when(mockChannel.toString()).thenReturn("nothing recognizable");
    assertThat(extract.bindingOf(mockChannel)).isEmpty();
  }

  @Test
  void extractDestinationBindingFound() {
    var b = new BindingProperties();
    b.setDestination("a-destination");
    when(bsp.getBindingProperties(anyString())).thenReturn(b);
    assertThat(extract.extractDestination(new NoOp( "a-channel"))).isEqualTo("a-destination");
  }

  @Test
  void extractDestinationBindingNotFound() {
    var channel = "my-channel";
    when(bsp.getBindingProperties(anyString())).thenReturn(null);
    assertThat(extract.extractDestination(new NoOp(channel))).isEqualTo(UNKNOWN);
  }

  @Test
  void extractGroupBindingFound() {
    var channel = "my-channel";
    var group = "a-group";
    var b = new BindingProperties();
    b.setGroup(group);
    when(bsp.getBindingProperties(anyString())).thenReturn(b);
    assertThat(extract.extractGroup(new NoOp(channel))).isEqualTo(group);
  }

  @Test
  void extractGroupBindingNotFound() {
    var channel = "my-channel";
    when(bsp.getBindingProperties(anyString())).thenReturn(null);
    assertThat(extract.extractGroup(new NoOp(channel))).isEqualTo(UNKNOWN);
  }

  @Test
  void extractPartitionIdFound() {
    MessageHeaders h = new MessageHeaders(Map.of(PARTITION_ID, "a-partition"));
    assertThat(extract.extractPartition(h)).isEqualTo("a-partition");
  }

  @Test
  void extractPartitionIdNotFoundRawIdFound() {
    MessageHeaders h = new MessageHeaders(Map.of(RAW_PARTITION_ID, "a-raw-partition"));
    assertThat(extract.extractPartition(h)).isEqualTo("a-raw-partition");
  }

  @Test
  void extractPartitionIdNotFoundRawIdNotFound() {
    assertThat(extract.extractPartition(new MessageHeaders(Map.of()))).isEqualTo(UNKNOWN);
  }

  @Test
  void extractPartitionIdTrumpsRawId() {
    MessageHeaders h = new MessageHeaders(
        Map.of(
            PARTITION_ID, "a-partition",
            RAW_PARTITION_ID, "a-raw-partition"));
    assertThat(extract.extractPartition(h)).isEqualTo("a-partition");
  }

  @Test
  void fetchBinderName() {
    var b = new BindingProperties();
    b.setBinder("a-binder");
    when(bsp.getBindingProperties("a-channel")).thenReturn(b);
    assertThat(extract.fetchBinderName(new NoOp("a-channel"))).isEqualTo("a-binder");
  }

  @Test
  void fetchBinderNameByDefaultCandidate() {
    var b = new BindingProperties();
    when(bsp.getDefaultBinder()).thenReturn("a-default-binder");
    when(bsp.getBindingProperties("a-channel")).thenReturn(b);
    assertThat(extract.fetchBinderName(new NoOp("a-channel"))).isEqualTo("a-default-binder");
  }

  @Test
  void fetchBinderNameBySearchingCandidate() {
    var first = new BinderProperties();
    first.setDefaultCandidate(false);
    var second = new BinderProperties();
    second.setDefaultCandidate(true);
    var b = new BindingProperties();
    when(bsp.getBinders()).thenReturn(Map.of("first", first, "second", second));
    when(bsp.getBindingProperties("a-channel")).thenReturn(b);
    assertThat(extract.fetchBinderName(new NoOp("a-channel"))).isEqualTo("second");
  }

  @Test
  void parseEnvironment() {
    assertThat(
        extract.parseEnvironment(buildEnv(Map.of("k-1", "v-1", "k-2", "v-2")))
    ).isEqualTo(Map.of("k-1", "v-1", "k-2", "v-2"));
  }

  @Test
  void parseEnvironmentBadEnv() {
    assertThat(
        extract.parseEnvironment(Map.of("k-1", "v-1", "k-2", "v-2"))
    ).isNull();
  }

  @Test
  void extractNamespaceNamespaceGiven() {
    var binding = new BindingProperties();
    binding.setBinder("a-binder");
    var binder = new BinderProperties();
    binder.setEnvironment(buildEnv(Map.of("namespace", "a-ns")));
    when(bsp.getBindingProperties("a-channel")).thenReturn(binding);
    when(bsp.getBinders()).thenReturn(Map.of("a-binder", binder));
    assertThat(extract.extractNamespace(new NoOp("a-channel"))).isEqualTo("a-ns");
  }

  @Test
  void extractNamespaceConnectionString() {
    var binding = new BindingProperties();
    binding.setBinder("a-binder");
    var binder = new BinderProperties();
    binder.setEnvironment(buildEnv(
        Map.of(
            "connection-string",
            "Endpoint=sb://a-namespace.servicebus.windows.net/;SharedAccessKeyName=Root")));
    when(bsp.getBindingProperties("a-channel")).thenReturn(binding);
    when(bsp.getBinders()).thenReturn(Map.of("a-binder", binder));
    assertThat(extract.extractNamespace(new NoOp("a-channel"))).isEqualTo("a-namespace");
  }

  @Test
  void extractNamespaceCannotParseConnectionString() {
    var binding = new BindingProperties();
    binding.setBinder("a-binder");
    var binder = new BinderProperties();
    binder.setEnvironment(buildEnv(Map.of("connection-string", "You can't parse this!")));
    when(bsp.getBindingProperties("a-channel")).thenReturn(binding);
    when(bsp.getBinders()).thenReturn(Map.of("a-binder", binder));
    assertThat(extract.extractNamespace(new NoOp("a-channel"))).isEqualTo(UNKNOWN);
  }

  @Test
  void extractNamespaceEnvironmentCannotBeParsed() {
    var binding = new BindingProperties();
    binding.setBinder("a-binder");
    var binder = new BinderProperties();
    binder.setEnvironment(Map.of("some", "mess"));
    when(bsp.getBindingProperties("a-channel")).thenReturn(binding);
    when(bsp.getBinders()).thenReturn(Map.of("a-binder", binder));
    assertThat(extract.extractNamespace(new NoOp("a-channel"))).isEqualTo(UNKNOWN);
  }

  private @NotNull @Unmodifiable Map<String, Object> buildEnv(Map<String, Object> values) {
    return  Map.of(
        "spring", Map.of(
            "cloud", Map.of(
                "azure", Map.of(
                    "eventhubs", values))));
  }

  /**
   * Mock channel
   */
  private static class NoOp extends AbstractMessageChannel {

    private final String name;

    public NoOp(String name) {
      this.name = name;
    }

    @Override
    protected boolean doSend(@NotNull Message<?> message, long timeout) {
      return true;
    }

    @Override
    public String getBeanName() {
      return name;
    }
  }

}