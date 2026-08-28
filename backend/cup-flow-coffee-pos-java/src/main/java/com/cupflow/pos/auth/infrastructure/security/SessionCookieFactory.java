package com.cupflow.pos.auth.infrastructure.security;

import com.cupflow.pos.auth.infrastructure.configuration.AuthSecurityProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieFactory {

    public static final String SESSION_COOKIE = "CUP_FLOW_SESSION";

    private final AuthSecurityProperties properties;

    public SessionCookieFactory(AuthSecurityProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie issue(String rawSessionToken) {
        return base(rawSessionToken).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(SESSION_COOKIE, value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Lax")
                .path("/");
    }
}
