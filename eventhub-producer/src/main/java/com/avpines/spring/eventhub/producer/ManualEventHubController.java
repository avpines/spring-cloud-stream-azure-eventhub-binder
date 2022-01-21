package com.avpines.spring.eventhub.producer;

import com.avpines.spring.messaging.SimpleEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Sinks.EmitFailureHandler;
import reactor.core.publisher.Sinks.Many;

@Slf4j
@RestController
@Profile("manual")
public class ManualEventHubController {

    private final Many<Message<SimpleEvent>> sink;

    public ManualEventHubController(Many<Message<SimpleEvent>> sink) {
        this.sink = sink;
    }

    @PostMapping("/send")
    public void delegateToEmitter(@RequestBody SimpleEvent body) {
        sink.emitNext(MessageBuilder.withPayload(body).build(), EmitFailureHandler.FAIL_FAST);
    }

}
