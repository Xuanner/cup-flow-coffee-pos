package com.cupflow.pos.shared.logging;

import org.slf4j.MDC;

public final class TraceContext {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER_NAME = "X-Request-Id";

    private TraceContext() {}

    public static String currentTraceId() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null ? "unavailable" : traceId;
    }
}
