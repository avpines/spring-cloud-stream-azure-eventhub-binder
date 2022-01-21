package com.avpines.spring.eventhub.producer;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Count the number of messages sent since last restarted
 */
@Slf4j
@Component
@GlobalChannelInterceptor
public class EventHubProducerChannelInterceptor implements ChannelInterceptor {

    private static final String SEND_TO = "spring.cloud.stream.sendto.destination";

    private static final String DEFAULT_BINDING = "default";

    private final AtomicInteger totalSent;

    private final Map<String, AtomicInteger> sentByBinding;

    public EventHubProducerChannelInterceptor() {
        this.totalSent = new AtomicInteger();
        this.sentByBinding = new TreeMap<>();
    }

    @Override
    public void afterSendCompletion(@NotNull Message<?> message,
                                    @NotNull MessageChannel channel,
                                    boolean sent,
                                    Exception ex) {
            if (sent) {
                // 'null' header can be sent to multiple beans, only one of them
                // matters, so make sure we only increase when that happens.
                if (channel.toString().matches(".*_integrationflow.channel#.*")) {
                    totalSent.incrementAndGet();
                    sentByBinding
                            .computeIfAbsent(sentTo(message), s -> new AtomicInteger())
                            .incrementAndGet();
                }
            }
    }

    @Scheduled(cron = "*/15 * * * * *")
    private void count() {
        LOG.info("Sent {} messages: {}", totalSent, sentByBinding);
    }

    private String sentTo(@NotNull Message<?> message) {
        String header = message.getHeaders().get(SEND_TO, String.class);
        return header != null ? header : DEFAULT_BINDING;
    }

}
