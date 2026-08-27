package com.cupflow.pos.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void normalizesUsernameWithoutChangingCase() {
        AccountUsername username = new AccountUsername("  Cashier01  ");

        assertThat(username.value()).isEqualTo("Cashier01");
    }

    @Test
    void rejectsBlankOrOversizedIdentityFields() {
        assertThatThrownBy(() -> new AccountUsername("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccountUsername("a".repeat(65))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordHash.of("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Account(
                        UUID.randomUUID(),
                        new AccountUsername("cashier"),
                        PasswordHash.of("test-only-hash"),
                        " ",
                        AccountStatus.ACTIVE,
                        Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redactsPasswordHashFromStringRepresentations() {
        String sensitiveHash = "test-only-sensitive-hash-value";
        PasswordHash passwordHash = PasswordHash.of(sensitiveHash);
        Account account = Account.newAccount(
                UUID.randomUUID(), new AccountUsername("cashier"), passwordHash, "测试收银员", AccountStatus.ACTIVE);

        assertThat(passwordHash.toString()).isEqualTo("[REDACTED]").doesNotContain(sensitiveHash);
        assertThat(account.toString()).doesNotContain(sensitiveHash);
    }
}
