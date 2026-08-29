package com.cupflow.pos.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class SecurityEventRecorderTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(SecurityEventRecorder.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void cleanUp() {
        logger.detachAppender(appender);
        appender.stop();
        MDC.clear();
    }

    @Test
    void recordsStableStructuredFieldsAndOmitsAbsentIdentity() {
        appender.start();
        logger.addAppender(appender);
        MDC.put(TraceContext.MDC_KEY, "security-event-unit-001");
        Instant now = Instant.parse("2026-08-29T12:00:00Z");
        UUID accountId = UUID.randomUUID();
        SecurityEventRecorder recorder = new SecurityEventRecorder(Clock.fixed(now, ZoneOffset.UTC));

        recorder.record(
                SecurityEventType.AUTHORIZATION_DENIED, SecurityEventOutcome.DENIED, accountId, "/api/v1/admin", null);

        assertThat(appender.list).singleElement().satisfies(event -> {
            Map<String, Object> fields = fields(event);
            assertThat(fields)
                    .containsEntry("securityEvent", "AUTHORIZATION_DENIED")
                    .containsEntry("outcome", "DENIED")
                    .containsEntry("eventTime", now)
                    .containsEntry("accountId", accountId)
                    .containsEntry("target", "/api/v1/admin")
                    .doesNotContainKey("reason");
            assertThat(event.getMDCPropertyMap()).containsEntry("traceId", "security-event-unit-001");
            assertThat(event.getFormattedMessage()).isEqualTo("Authentication security event");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    private Map<String, Object> fields(ILoggingEvent event) {
        return event.getKeyValuePairs().stream().collect(Collectors.toMap(pair -> pair.key, pair -> pair.value));
    }
}
