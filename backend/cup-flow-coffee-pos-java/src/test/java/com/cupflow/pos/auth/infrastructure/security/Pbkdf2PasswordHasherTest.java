package com.cupflow.pos.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cupflow.pos.auth.domain.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Pbkdf2PasswordHasherTest {

    private final Pbkdf2PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();

    @Test
    @DisplayName("TC-S2-PASS-001 正确密码能够通过安全校验")
    void tcS2Pass001AcceptsTheCorrectPassword() {
        String testPassword = "test-only-correct-password";
        PasswordHash passwordHash = passwordHasher.hash(testPassword);

        assertThat(passwordHasher.matches(testPassword, passwordHash)).isTrue();
    }

    @Test
    @DisplayName("TC-S2-PASS-002 错误密码返回不匹配")
    void tcS2Pass002RejectsAnIncorrectPassword() {
        PasswordHash passwordHash = passwordHasher.hash("test-only-correct-password");

        assertThat(passwordHasher.matches("test-only-incorrect-password", passwordHash))
                .isFalse();
    }

    @Test
    @DisplayName("TC-S2-PASS-003 相同密码生成不同摘要且均可验证")
    void tcS2Pass003CreatesDifferentSelfDescribingHashesThatBothVerify() {
        String testPassword = "test-only-password-value";

        PasswordHash first = passwordHasher.hash(testPassword);
        PasswordHash second = passwordHasher.hash(testPassword);

        assertThat(first).isNotEqualTo(second);
        assertThat(first.value())
                .startsWith("$pbkdf2-sha256$" + Pbkdf2PasswordHasher.ITERATIONS + "$")
                .doesNotContain(testPassword)
                .hasSizeLessThanOrEqualTo(255);
        assertThat(second.value()).doesNotContain(testPassword);
        assertThat(passwordHasher.matches(testPassword, first)).isTrue();
        assertThat(passwordHasher.matches(testPassword, second)).isTrue();
    }

    @Test
    void supportsTheContractMaximumPasswordLength() {
        PasswordHash passwordHash = passwordHasher.hash("x".repeat(128));

        assertThat(passwordHash.value())
                .startsWith("$pbkdf2-sha256$" + Pbkdf2PasswordHasher.ITERATIONS + "$")
                .hasSizeLessThanOrEqualTo(255);
    }

    @Test
    @DisplayName("TC-S2-PASS-005 损坏或不支持的摘要安全失败且不泄露内容")
    void tcS2Pass005RejectsMalformedOrUnsupportedHashesWithoutLeakingThem() {
        String malformedHash = "$pbkdf2-sha256$1$invalid-salt$invalid-hash";

        assertThatCode(() -> passwordHasher.matches("test-only-password", PasswordHash.of(malformedHash)))
                .doesNotThrowAnyException();
        assertThat(passwordHasher.matches("test-only-password", PasswordHash.of(malformedHash)))
                .isFalse();
        assertThat(passwordHasher.matches(
                        "test-only-password", PasswordHash.of("$pbkdf2-sha512$600000$invalid$invalid")))
                .isFalse();
        assertThatThrownBy(() -> passwordHasher.hash("x".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("x".repeat(129));
    }
}
