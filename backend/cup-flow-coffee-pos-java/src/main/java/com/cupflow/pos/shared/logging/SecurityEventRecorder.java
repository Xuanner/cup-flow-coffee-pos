package com.cupflow.pos.shared.logging;

import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventRecorder {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventRecorder.class);

    private final Clock clock;

    public SecurityEventRecorder(Clock clock) {
        this.clock = clock;
    }

    public void record(
            SecurityEventType eventType, SecurityEventOutcome outcome, UUID accountId, String target, String reason) {
        LoggingEventBuilder event = log.atInfo()
                .addKeyValue("securityEvent", eventType.name())
                .addKeyValue("outcome", outcome.name())
                .addKeyValue("eventTime", clock.instant());
        if (accountId != null) {
            event.addKeyValue("accountId", accountId);
        }
        if (target != null && !target.isBlank()) {
            event.addKeyValue("target", target);
        }
        if (reason != null && !reason.isBlank()) {
            event.addKeyValue("reason", reason);
        }
        event.log("Authentication security event");
    }
}
