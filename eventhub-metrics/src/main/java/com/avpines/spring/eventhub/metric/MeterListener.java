package com.avpines.spring.eventhub.metric;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

public interface MeterListener {

  void capture(
      @NotNull MessageHeaders headers,
      @NotNull MessageChannel channel,
      boolean sent,
      @Nullable Exception ex);
}