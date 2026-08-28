package com.cupflow.pos.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.cupflow.pos.auth.infrastructure.configuration.AuthSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SessionCookieFactoryTest {

    @Test
    @DisplayName("TASK-S2-SESSION-01-02 生产会话 Cookie 使用冻结的安全属性")
    void issuesSecureHostOnlySessionCookieInProductionMode() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setCookieSecure(true);
        SessionCookieFactory factory = new SessionCookieFactory(properties);

        String cookie = factory.issue("test-only-session-token").toString();

        assertThat(cookie)
                .contains("CUP_FLOW_SESSION=", "Path=/", "Secure", "HttpOnly", "SameSite=Lax")
                .doesNotContain("Domain=", "Max-Age=", "Expires=");
    }

    @Test
    @DisplayName("TASK-S2-SESSION-01-02 本地 Cookie 可使用 HTTP 且清除时立即过期")
    void supportsLocalHttpAndSafeClearing() {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setCookieSecure(false);
        SessionCookieFactory factory = new SessionCookieFactory(properties);

        assertThat(factory.issue("test-only-session-token").toString()).doesNotContain("Secure", "Domain=");
        assertThat(factory.clear().toString())
                .contains("CUP_FLOW_SESSION=", "Max-Age=0", "Path=/", "HttpOnly", "SameSite=Lax")
                .doesNotContain("Domain=");
    }
}
