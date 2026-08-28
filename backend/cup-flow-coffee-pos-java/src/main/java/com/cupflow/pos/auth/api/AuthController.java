package com.cupflow.pos.auth.api;

import com.cupflow.pos.auth.application.CurrentSessionResult;
import com.cupflow.pos.auth.application.CurrentSessionService;
import com.cupflow.pos.auth.application.LoginResult;
import com.cupflow.pos.auth.application.LoginService;
import com.cupflow.pos.auth.application.LogoutService;
import com.cupflow.pos.auth.infrastructure.security.CsrfTokenService;
import com.cupflow.pos.auth.infrastructure.security.SessionCookieFactory;
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

    public static final String SESSION_COOKIE = SessionCookieFactory.SESSION_COOKIE;

    private final LoginService loginService;
    private final LogoutService logoutService;
    private final CurrentSessionService currentSessionService;
    private final CsrfTokenService csrfTokenService;
    private final SessionCookieFactory sessionCookieFactory;

    public AuthController(
            LoginService loginService,
            LogoutService logoutService,
            CurrentSessionService currentSessionService,
            CsrfTokenService csrfTokenService,
            SessionCookieFactory sessionCookieFactory) {
        this.loginService = loginService;
        this.logoutService = logoutService;
        this.currentSessionService = currentSessionService;
        this.csrfTokenService = csrfTokenService;
        this.sessionCookieFactory = sessionCookieFactory;
    }

    @GetMapping("/me")
    ResponseEntity<ApiResponse<Object>> me(
            @CookieValue(name = SESSION_COOKIE, required = false) String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        CurrentSessionResult result = currentSessionService.resolve(rawSessionToken);
        if (result instanceof CurrentSessionResult.Invalid) {
            ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
            return ResponseEntity.status(errorCode.status())
                    .header(HttpHeaders.SET_COOKIE, sessionCookieFactory.clear().toString())
                    .body(ApiResponse.failure(
                            errorCode.code(), errorCode.message(), null, TraceContext.currentTraceId()));
        }
        CurrentSessionResult.Authenticated authenticated = (CurrentSessionResult.Authenticated) result;
        return ResponseEntity.ok(ApiResponse.success(authenticated.currentUser(), TraceContext.currentTraceId()));
    }

    @GetMapping("/csrf")
    ApiResponse<CsrfTokenResponse> csrf() {
        return ApiResponse.success(
                new CsrfTokenResponse(CsrfTokenService.HEADER_NAME, csrfTokenService.issue()),
                TraceContext.currentTraceId());
    }

    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Object>> logout(
            @CookieValue(name = SESSION_COOKIE, required = false) String rawSessionToken) {
        logoutService.logout(rawSessionToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieFactory.clear().toString())
                .body(ApiResponse.success(null, TraceContext.currentTraceId()));
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
        ResponseCookie cookie = sessionCookieFactory.issue(success.sessionToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(success.currentUser(), TraceContext.currentTraceId()));
    }
}
