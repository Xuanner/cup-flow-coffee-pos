package com.cupflow.pos.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cupflow.pos.auth.domain.RoleCode;
import org.junit.jupiter.api.Test;

class BootstrapAccountTest {

    @Test
    void validatesAndNormalizesBootstrapInputWithoutChangingThePassword() {
        BootstrapAccount account = BootstrapAccount.of("  Cashier01  ", "  test-password  ", "测试收银员", RoleCode.CASHIER);

        assertThat(account.username().value()).isEqualTo("Cashier01");
        assertThat(account.password()).isEqualTo("  test-password  ");
    }

    @Test
    void rejectsMissingOrInvalidFieldsWithoutIncludingTheirValues() {
        assertThatThrownBy(() -> BootstrapAccount.of(null, "test-password", "测试", RoleCode.CASHIER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("test-password");
        assertThatThrownBy(() -> BootstrapAccount.of("cashier", "short", "测试", RoleCode.CASHIER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("short");
        assertThatThrownBy(() -> BootstrapAccount.of("cashier", "test-password", " ", RoleCode.CASHIER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("test-password");
    }

    @Test
    void redactsCredentialsFromStringRepresentation() {
        BootstrapAccount account =
                BootstrapAccount.of("sensitive-username", "sensitive-password", "敏感展示名", RoleCode.ADMIN);

        assertThat(account.toString())
                .doesNotContain("sensitive-username", "sensitive-password", "敏感展示名")
                .contains("[REDACTED]", "ADMIN");
    }
}
