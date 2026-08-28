package com.cupflow.pos.auth.infrastructure.configuration;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.security")
public class AuthSecurityProperties {

    private boolean cookieSecure;
    private List<String> allowedOrigins = List.of("http://localhost:5173");

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("At least one authentication origin must be configured");
        }
        this.allowedOrigins = allowedOrigins.stream().map(this::validatedOrigin).toList();
    }

    private String validatedOrigin(String value) {
        String origin = value == null ? "" : value.strip();
        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Authentication origin must be an absolute HTTP origin", exception);
        }
        boolean supportedScheme = "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
        if (origin.contains("*")
                || !supportedScheme
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty())
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Authentication origin must be an absolute HTTP origin without wildcard");
        }
        return origin;
    }
}
