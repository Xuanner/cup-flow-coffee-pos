package com.cupflow.pos.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cupflow.pos.auth.domain.PasswordHash;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveValueScannerTest {

    private static final String TEST_SECRET = "test-only-sensitive-value-7f4c";

    @Test
    @DisplayName("TC-S2-AUDIT-010 注入测试敏感值时扫描器失败且不回显")
    void tcS2Audit010DetectsAnInjectedTestSensitiveValueWithoutEchoingIt() {
        assertThatThrownBy(() -> SensitiveValueScanner.assertAbsent(
                        Map.of("injected-artifact", "prefix " + TEST_SECRET + " suffix"), List.of(TEST_SECRET)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("injected-artifact")
                .hasMessageNotContaining(TEST_SECRET);
    }

    @Test
    @DisplayName("TC-S2-AUDIT-009 正常认证产物不包含密码或摘要")
    void tcS2Audit009AcceptsRedactedPasswordArtifacts() {
        PasswordHash passwordHash = PasswordHash.of("test-only-stored-password-hash");
        Map<String, String> artifacts = Map.of(
                "password-hash-to-string", passwordHash.toString(),
                "safe-exception", new IllegalArgumentException("Password verification failed").toString(),
                "safe-log", "Password verification completed without credential fields");

        assertThatCode(() -> SensitiveValueScanner.assertAbsent(artifacts, List.of(TEST_SECRET, passwordHash.value())))
                .doesNotThrowAnyException();
    }
}
