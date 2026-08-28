package com.cupflow.pos.auth.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthSecurityPropertiesTest {

    @Test
    @DisplayName("TASK-S2-SESSION-01-02 允许明确的 HTTP 与 HTTPS Origin")
    void acceptsExplicitHttpOrigins() {
        AuthSecurityProperties properties = new AuthSecurityProperties();

        properties.setAllowedOrigins(List.of("http://localhost:5173", "https://pos.example.com"));

        assertThat(properties.getAllowedOrigins()).containsExactly("http://localhost:5173", "https://pos.example.com");
    }

    @Test
    @DisplayName("TASK-S2-SESSION-01-02 拒绝通配、路径和非 HTTP Origin")
    void rejectsWildcardAndNonOriginValues() {
        assertThatThrownBy(() -> new AuthSecurityProperties().setAllowedOrigins(List.of("*")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> new AuthSecurityProperties().setAllowedOrigins(List.of("https://pos.example.com/path")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthSecurityProperties().setAllowedOrigins(List.of("file:///tmp/pos")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
