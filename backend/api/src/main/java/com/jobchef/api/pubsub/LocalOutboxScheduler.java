package com.jobchef.api.pubsub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jobchef.mode", havingValue = "demo")
public class LocalOutboxScheduler {
    private final OutboxDispatchService outbox;

    public LocalOutboxScheduler(OutboxDispatchService outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelayString = "${jobchef.outbox-local-poll-ms:3000}")
    public void dispatchLocalOutbox() {
        outbox.dispatch();
    }
}
