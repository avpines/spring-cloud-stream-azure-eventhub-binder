package com.avpines.spring.eventhub.transformer;

import static com.azure.spring.messaging.AzureHeaders.CHECKPOINTER;

import com.azure.spring.messaging.checkpoint.Checkpointer;
import java.util.Optional;
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
@GlobalChannelInterceptor(patterns = "dlq-*")
public class DlqInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
    LOG.info("[DLQ]-> preSend '{}' to '{}'", message.getHeaders().get("id"), channel);
    return ChannelInterceptor.super.preSend(message, channel);
  }

  @Override
  public void postSend(@NotNull Message<?> message, @NotNull MessageChannel channel, boolean sent) {
    LOG.info("[DLQ]-> postSend '{}' to '{}', sent={}", message.getHeaders().get("id"), channel, sent);
    ChannelInterceptor.super.postSend(message, channel, sent);
  }

  @Override
  public void afterSendCompletion(
      @NotNull Message<?> message,
      @NotNull MessageChannel channel,
      boolean sent,
      Exception ex) {
    LOG.info("[DLQ]-> afterSendCompletion '{}' to '{}', sent={}, ex='{}'",
        message, channel, sent, ex != null ? ex.getMessage() : "-");
    if (sent) {
      Optional.ofNullable((Checkpointer) message.getHeaders().get(CHECKPOINTER))
          .map(c -> c.success()
              .doOnSuccess(s -> LOG.info("[DLQ]-> Message successfully checkpointed: '{}'", message.getHeaders().get("id")))
              .doOnError(error -> LOG.error("[DLQ]-> Checkpoint failed", error))
              .subscribe());
    } else {
      LOG.info("Message was not sent, will not be checkpointed: '{}'", message.getHeaders().get("id"));
    }
  }

}

