package com.avpines.spring.eventhub.transformer;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Service;

/**
 * Channel interceptor for the incoming messages. {@link #afterSendCompletion(Message,
 * MessageChannel, boolean, Exception)} will be invoked _after_ the message was sent through the
 * output channel. If the message was not sent through the output channel (on exception) 'sent' will
 * be false. When a message is received the order of calls will be (IN - incoming channel OUT -
 * outgoing channel):
 * <pre>
 *   IN.preSend ->
 *   {@link EventHubTransformerDlq#transform()} (businness logic) ->
 *   OUT.preSend ->
 *   (Send message to next channel) ->
 *   OUT.postSend ->
 *   OUT.afterSendCompletion ->
 *   IN.postSend ->
 *   IN.afterSendCompletion
 * </pre>
 */
@Slf4j
@Service
@GlobalChannelInterceptor(patterns = "transform-out-*")
public class TransformerOutputInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
    LOG.info("[OUT]-> preSend '{}' to '{}'", message.getHeaders().get("id"), channel);
    return ChannelInterceptor.super.preSend(message, channel);
  }

  @Override
  public void postSend(@NotNull Message<?> message, @NotNull MessageChannel channel, boolean sent) {
    LOG.info("[OUT]-> postSend '{}' to '{}', sent={}",  message.getHeaders().get("id"), channel, sent);
    ChannelInterceptor.super.postSend(message, channel, sent);
  }

  @Override
  public void afterSendCompletion(
      @NotNull Message<?> message,
      @NotNull MessageChannel channel,
      boolean sent,
      Exception ex) {
    LOG.info("[OUT]-> afterSendCompletion '{}' to '{}', sent={}, ex='{}'",
        message.getHeaders().get("id"), channel, sent, ex != null ? ex.getMessage() : "-");
  }

}
