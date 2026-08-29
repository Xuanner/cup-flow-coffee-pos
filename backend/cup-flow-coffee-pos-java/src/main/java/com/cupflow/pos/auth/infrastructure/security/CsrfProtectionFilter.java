package com.cupflow.pos.auth.infrastructure.security;

import com.cupflow.pos.auth.infrastructure.configuration.AuthSecurityProperties;
import com.cupflow.pos.shared.error.ErrorCode;
import com.cupflow.pos.shared.logging.SecurityEventOutcome;
import com.cupflow.pos.shared.logging.SecurityEventRecorder;
import com.cupflow.pos.shared.logging.SecurityEventType;
import com.cupflow.pos.shared.logging.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CsrfProtectionFilter extends OncePerRequestFilter {

    private final CsrfTokenService tokenService;
    private final SecurityEventRecorder securityEventRecorder;
    private final Set<String> allowedOrigins;

    public CsrfProtectionFilter(
            CsrfTokenService tokenService,
            AuthSecurityProperties properties,
            SecurityEventRecorder securityEventRecorder) {
        this.tokenService = tokenService;
        this.allowedOrigins = new HashSet<>(properties.getAllowedOrigins());
        this.securityEventRecorder = securityEventRecorder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !"/api/v1/auth/login".equals(path) && !"/api/v1/auth/logout".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        boolean originAllowed = origin == null || allowedOrigins.contains(origin) || sameOrigin(request, origin);
        if (!originAllowed || !tokenService.isValid(request.getHeader(CsrfTokenService.HEADER_NAME))) {
            securityEventRecorder.record(
                    SecurityEventType.CSRF_REJECTED,
                    SecurityEventOutcome.DENIED,
                    null,
                    request.getRequestURI(),
                    "VALIDATION_FAILED");
            ErrorCode errorCode = ErrorCode.SECURITY_VALIDATION_FAILED;
            response.setStatus(errorCode.status().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                            {"code":"%s","message":"%s","data":null,"traceId":"%s","timestamp":"%s"}
                            """.formatted(
                            errorCode.code(), errorCode.message(), TraceContext.currentTraceId(), Instant.now()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean sameOrigin(HttpServletRequest request, String origin) {
        String defaultPort = request.isSecure() ? "443" : "80";
        String port = request.getServerPort() == Integer.parseInt(defaultPort) ? "" : ":" + request.getServerPort();
        return origin.equals(request.getScheme() + "://" + request.getServerName() + port);
    }
}
