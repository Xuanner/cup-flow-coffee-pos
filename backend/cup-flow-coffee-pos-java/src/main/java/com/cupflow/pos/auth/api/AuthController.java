package com.cupflow.pos.auth.api;

import com.cupflow.pos.auth.application.LoginResult;
import com.cupflow.pos.auth.application.LoginService;
import com.cupflow.pos.auth.infrastructure.configuration.AuthSecurityProperties;
import com.cupflow.pos.auth.infrastructure.security.CsrfTokenService;
import com.cupflow.pos.shared.api.ApiResponse;
import com.cupflow.pos.shared.error.ApiException;
import com.cupflow.pos.shared.error.ErrorCode;
import com.cupflow.pos.shared.logging.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public static final String SESSION_COOKIE = "CUP_FLOW_SESSION";

    private final LoginService loginService;
    private final CsrfTokenService csrfTokenService;
    private final AuthSecurityProperties securityProperties;

    public AuthController(
            LoginService loginService, CsrfTokenService csrfTokenService, AuthSecurityProperties securityProperties) {
        this.loginService = loginService;
        this.csrfTokenService = csrfTokenService;
        this.securityProperties = securityProperties;
    }

    @GetMapping("/csrf")
    ApiResponse<CsrfTokenResponse> csrf() {
        return ApiResponse.success(
                new CsrfTokenResponse(CsrfTokenService.HEADER_NAME, csrfTokenService.issue()),
                TraceContext.currentTraceId());
    }

    @PostMapping("/login")
    ResponseEntity<ApiResponse<Object>> login(
            @Valid @RequestBody LoginRequest request,
            @CookieValue(name = SESSION_COOKIE, required = false) String previousSessionToken,
            HttpServletRequest httpRequest) {
        LoginResult result = loginService.login(
                request.username(), request.password(), previousSessionToken, httpRequest.getRemoteAddr());
        if (result instanceof LoginResult.RateLimited rateLimited) {
            ErrorCode errorCode = ErrorCode.AUTHENTICATION_RATE_LIMITED;
            return ResponseEntity.status(errorCode.status())
                    .header(HttpHeaders.RETRY_AFTER, Long.toString(rateLimited.retryAfterSeconds()))
                    .body(ApiResponse.failure(
                            errorCode.code(), errorCode.message(), null, TraceContext.currentTraceId()));
        }
        if (result instanceof LoginResult.Failure) {
            throw new ApiException(ErrorCode.AUTHENTICATION_FAILED);
        }
        LoginResult.Success success = (LoginResult.Success) result;
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, success.sessionToken())
                .httpOnly(true)
                .secure(securityProperties.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(success.currentUser(), TraceContext.currentTraceId()));
    }
}
