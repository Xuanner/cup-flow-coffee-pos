package com.cupflow.pos.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZSensitiveArtifactScanTest {

    private static final List<String> TEST_SENSITIVE_VALUES = List.of(
            "test-only-audit-cashier",
            "test-only-audit-admin",
            "test-only-audit-unknown",
            "test-only-audit-limited",
            "test-only-audit-disabled",
            "test-only-invalid-csrf");

    @Test
    @DisplayName("TC-S2-AUDIT-009 测试报告与示例配置不包含审计旅程凭证")
    void scansGeneratedReportsAndExampleConfiguration() throws Exception {
        Map<String, String> reports = SensitiveValueScanner.readTextArtifacts(Path.of("target/surefire-reports"));
        Map<String, String> exampleConfiguration = Map.of(
                "backend-env-example", java.nio.file.Files.readString(Path.of(".env.example")),
                "application-config", java.nio.file.Files.readString(Path.of("src/main/resources/application.yml")));

        assertThatCode(() -> {
                    SensitiveValueScanner.assertAbsent(reports, TEST_SENSITIVE_VALUES);
                    SensitiveValueScanner.assertAbsent(exampleConfiguration, TEST_SENSITIVE_VALUES);
                })
                .doesNotThrowAnyException();
    }
}
