package com.avpines.spring.eventhub.metric;

import static com.azure.spring.messaging.AzureHeaders.PARTITION_ID;
import static com.azure.spring.messaging.AzureHeaders.RAW_PARTITION_ID;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.config.BinderProperties;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

@Service
public class EventHubPropertyExtractor {

  private static final String UNKNOWN = "UNKNOWN";
  private static final String[] ENV_PATH = new String[]{"spring", "cloud", "azure", "eventhubs"};
  private static final String NAMESPACE = "namespace";
  private static final String CONNECTION_STRING = "connection-string";

  private BindingServiceProperties bindingServiceProperties;

  @Autowired
  public final void setBindingServiceProperties(BindingServiceProperties bindingServiceProperties) {
    this.bindingServiceProperties = bindingServiceProperties;
  }

  public @NotNull String extractDestination(@NotNull MessageChannel channel) {
    return bindingOf(channel).map(BindingProperties::getDestination).orElse(UNKNOWN);
  }

  public @NotNull String extractGroup(@NotNull MessageChannel channel) {
    return bindingOf(channel).map(BindingProperties::getGroup).orElse(UNKNOWN);
  }

  public @NotNull String extractPartition(@NotNull MessageHeaders headers) {
    return Optional.ofNullable(headers.get(PARTITION_ID, String.class))
        .or(() -> Optional.ofNullable(headers.get(RAW_PARTITION_ID, String.class)))
        .orElse(UNKNOWN);
  }

  public @NotNull String extractNamespace(@NotNull MessageChannel channel) {
    return Optional.ofNullable(bindingServiceProperties.getBinders().get(fetchBinderName(channel)))
        .map(BinderProperties::getEnvironment)
        .map(this::parseEnvironment)
        .flatMap(
            m -> parseConnectionString((String) m.get(CONNECTION_STRING))
                .or(() -> Optional.ofNullable((String) m.get(NAMESPACE))))
        .orElse(UNKNOWN);
  }

  @NotNull String fetchBinderName(@NotNull MessageChannel channel) {
    return bindingOf(channel).map(bp -> {
      if (bp.getBinder() != null) {
        return bp.getBinder();
      }
      return bindingServiceProperties.getDefaultBinder() != null
          ? bindingServiceProperties.getDefaultBinder() : fetchDefaultCandidate();
    }).orElse(channel.toString());
  }

  Optional<String> parseConnectionString(@Nullable String connectionString) {
    return Optional.ofNullable(connectionString)
        .map(s -> {
          String replaced = s.replaceAll(".*sb://(.*?)/", "$1");
          // if the regex doesn't match, we potentially can return the full connection string, that
          // in most cases is a secret that we don't want to use as a tag. To be safe, if this
          // happens (and no, it shouldn't), we will return UNKNOWN.
          return replaced.equals(s) ? UNKNOWN : replaced;
        })
        .map(s -> s.split("\\.")[0]);
  }

  @SuppressWarnings("unchecked")
  @Nullable Map<String, Object> parseEnvironment(Map<String, Object> environment) {
    Map<String, Object> env = environment;
    for (String value : ENV_PATH) {
      env = (Map<String, Object>) env.get(value);
      if (env == null) {
        return null;
      }
    }
    return env;
  }

  Optional<BindingProperties> bindingOf(@NotNull MessageChannel channel) {
    return Optional.ofNullable(
        bindingServiceProperties.getBindingProperties(
            AbstractMessageChannel.class.isAssignableFrom(channel.getClass())
                ? ((AbstractMessageChannel) channel).getBeanName()
                // if we can't get the bean name, we will attempt to parse it. the MessageChannel's
                // toString() outputs the name in the format "bean 'channelName'"
                : channel.toString().replaceAll(".*'(.*?)'", "$1")));
  }

  private @NotNull String fetchDefaultCandidate() {
    // Spring Cloud Stream ensures that we will have a single default candidate, and fails if
    // none were found / more than one was found.
    return bindingServiceProperties.getBinders().entrySet().stream()
        .filter(e -> e.getValue().isDefaultCandidate()).map(Entry::getKey)
        .collect(Collectors.toList()).get(0);
  }

}
