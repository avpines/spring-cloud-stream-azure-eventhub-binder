package com.avpines.spring.eventhub.metric;

import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@GlobalChannelInterceptor
public class BindingMeterInterceptor implements ChannelInterceptor {

  private final Collection<MeterListener> listeners;

  public BindingMeterInterceptor(@NotNull Collection<MeterListener> listeners) {
    this.listeners = listeners;
  }

  @Override
  public void afterSendCompletion(
      @NotNull Message<?> message,
      @NotNull MessageChannel channel,
      boolean sent,
      Exception ex) {
    this.listeners.forEach(listener -> listener.capture(message.getHeaders(), channel, sent, ex));
  }

}