package com.cupflow.pos.auth.infrastructure.security;

import com.cupflow.pos.auth.api.AuthController;
import com.cupflow.pos.auth.application.CurrentSessionResult;
import com.cupflow.pos.auth.application.CurrentSessionService;
import com.cupflow.pos.auth.application.CurrentUser;
import com.cupflow.pos.auth.application.CurrentUserContext;
import com.cupflow.pos.auth.application.RoleAuthorization;
import com.cupflow.pos.shared.api.ApiResponse;
import com.cupflow.pos.shared.error.ErrorCode;
import com.cupflow.pos.shared.logging.TraceContext;
import com.cupflow.pos.shared.security.AuthenticatedEndpoint;
import com.cupflow.pos.shared.security.PublicEndpoint;
import com.cupflow.pos.shared.security.RequiresRole;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final CurrentSessionService currentSessionService;
    private final CurrentUserContext currentUserContext;
    private final RoleAuthorization roleAuthorization;
    private final SessionCookieFactory sessionCookieFactory;
    private final JsonMapper jsonMapper;

    public AuthorizationInterceptor(
            CurrentSessionService currentSessionService,
            CurrentUserContext currentUserContext,
            RoleAuthorization roleAuthorization,
            SessionCookieFactory sessionCookieFactory,
            JsonMapper jsonMapper) {
        this.currentSessionService = currentSessionService;
        this.currentUserContext = currentUserContext;
        this.roleAuthorization = roleAuthorization;
        this.sessionCookieFactory = sessionCookieFactory;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        currentUserContext.clear();
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (hasAnnotation(handlerMethod, PublicEndpoint.class)) {
            return true;
        }

        String rawSessionToken = sessionCookie(request);
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            writeFailure(response, ErrorCode.UNAUTHENTICATED, false);
            return false;
        }
        CurrentSessionResult result = currentSessionService.resolve(rawSessionToken);
        if (!(result instanceof CurrentSessionResult.Authenticated authenticated)) {
            writeFailure(response, ErrorCode.UNAUTHENTICATED, true);
            return false;
        }

        CurrentUser currentUser = authenticated.currentUser();
        currentUserContext.set(currentUser);
        if (hasAnnotation(handlerMethod, AuthenticatedEndpoint.class)) {
            return true;
        }
        RequiresRole requiresRole = annotation(handlerMethod, RequiresRole.class);
        if (requiresRole != null && roleAuthorization.allows(currentUser, requiresRole.value())) {
            return true;
        }

        currentUserContext.clear();
        writeFailure(response, ErrorCode.FORBIDDEN, false);
        return false;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        currentUserContext.clear();
    }

    private String sessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AuthController.SESSION_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private <A extends java.lang.annotation.Annotation> boolean hasAnnotation(
            HandlerMethod handlerMethod, Class<A> annotationType) {
        return annotation(handlerMethod, annotationType) != null;
    }

    private <A extends java.lang.annotation.Annotation> A annotation(
            HandlerMethod handlerMethod, Class<A> annotationType) {
        A methodAnnotation = handlerMethod.getMethodAnnotation(annotationType);
        return methodAnnotation != null
                ? methodAnnotation
                : handlerMethod.getBeanType().getAnnotation(annotationType);
    }

    private void writeFailure(HttpServletResponse response, ErrorCode errorCode, boolean clearCookie) throws Exception {
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (clearCookie) {
            response.addHeader(
                    HttpHeaders.SET_COOKIE, sessionCookieFactory.clear().toString());
        }
        jsonMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(errorCode.code(), errorCode.message(), null, TraceContext.currentTraceId()));
    }
}
