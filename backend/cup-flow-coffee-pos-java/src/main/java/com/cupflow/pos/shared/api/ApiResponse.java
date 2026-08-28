package com.cupflow.pos.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

public record ApiResponse<T>(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.ALWAYS) T data,
        String traceId,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("SUCCESS", "操作成功", data, traceId, Instant.now());
    }

    public static ApiResponse<Object> failure(String code, String message, Object data, String traceId) {
        return new ApiResponse<>(code, message, data, traceId, Instant.now());
    }
}
