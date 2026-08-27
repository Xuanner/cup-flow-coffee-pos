package com.cupflow.pos.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_ARGUMENT("COMMON-400-001", "请求参数不正确", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED("AUTH-401-001", "登录状态已失效，请重新登录", HttpStatus.UNAUTHORIZED),
    AUTHENTICATION_FAILED("AUTH-401-002", "账号或密码错误，或账号不可用", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH-403-001", "没有执行此操作的权限", HttpStatus.FORBIDDEN),
    SECURITY_VALIDATION_FAILED("AUTH-403-002", "请求安全校验失败，请刷新后重试", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("COMMON-404-001", "请求的资源不存在", HttpStatus.NOT_FOUND),
    STATE_CONFLICT("COMMON-409-001", "数据状态已变化，请刷新后重试", HttpStatus.CONFLICT),
    INTERNAL_ERROR("COMMON-500-001", "服务暂时不可用，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}
