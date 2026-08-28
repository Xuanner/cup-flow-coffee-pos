package com.cupflow.pos.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cupflow.pos.shared.api.ApiResponse;
import com.cupflow.pos.shared.logging.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private static final String TRACE_ID = "auth-error-contract-001";
    private static final String TEST_SECRET = "test-only-unexpected-secret";

    @AfterEach
    void clearTraceContext() {
        MDC.clear();
    }

    @Test
    @DisplayName("TASK-S2-AUTH-02-03 系统错误保持统一结构且响应与日志脱敏")
    void returnsSafeUnexpectedErrorWithoutLoggingTheExceptionPayloadOrStack() {
        MDC.put(TraceContext.MDC_KEY, TRACE_ID);
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ResponseEntity<ApiResponse<Object>> response =
                    new GlobalExceptionHandler().handleUnexpected(new IllegalStateException(TEST_SECRET));

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull().satisfies(body -> {
                assertThat(body.code()).isEqualTo("COMMON-500-001");
                assertThat(body.message()).isEqualTo("服务暂时不可用，请稍后重试");
                assertThat(body.data()).isNull();
                assertThat(body.traceId()).isEqualTo(TRACE_ID);
                assertThat(body.timestamp()).isNotNull();
                assertThat(body.toString()).doesNotContain(TEST_SECRET, "Exception", "password", "token");
            });
            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getFormattedMessage())
                        .contains(TRACE_ID, "IllegalStateException")
                        .doesNotContain(TEST_SECRET);
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
