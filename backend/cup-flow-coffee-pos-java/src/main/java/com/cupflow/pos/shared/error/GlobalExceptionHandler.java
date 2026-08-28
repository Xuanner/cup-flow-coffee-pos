package com.cupflow.pos.shared.error;

import com.cupflow.pos.shared.api.ApiResponse;
import com.cupflow.pos.shared.logging.TraceContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Object>> handleApiException(ApiException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.failure(errorCode.code(), errorCode.message(), null, TraceContext.currentTraceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        ErrorCode errorCode = ErrorCode.INVALID_ARGUMENT;
        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.failure(
                        errorCode.code(), errorCode.message(), violations, TraceContext.currentTraceId()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Object>> handleNotFound(NoResourceFoundException exception) {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.failure(errorCode.code(), errorCode.message(), null, TraceContext.currentTraceId()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception exception) {
        String traceId = TraceContext.currentTraceId();
        log.error(
                "Unhandled request failure, traceId={}, type={}",
                traceId,
                exception.getClass().getSimpleName());
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.failure(errorCode.code(), errorCode.message(), null, traceId));
    }

    private FieldViolation toViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }
}
