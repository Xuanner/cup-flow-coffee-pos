package com.cupflow.pos.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.cupflow.pos.auth.domain.PasswordHash;
import org.junit.jupiter.api.Test;

class Pbkdf2PasswordHasherTest {

    private final Pbkdf2PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();

    @Test
    void createsDifferentSelfDescribingHashesForTheSamePassword() {
        String testPassword = "test-only-password-value";

        PasswordHash first = passwordHasher.hash(testPassword);
        PasswordHash second = passwordHasher.hash(testPassword);

        assertThat(first).isNotEqualTo(second);
        assertThat(first.value())
                .startsWith("$pbkdf2-sha256$" + Pbkdf2PasswordHasher.ITERATIONS + "$")
                .doesNotContain(testPassword)
                .hasSizeLessThanOrEqualTo(255);
        assertThat(second.value()).doesNotContain(testPassword);
    }

    @Test
    void supportsTheContractMaximumPasswordLength() {
        PasswordHash passwordHash = passwordHasher.hash("x".repeat(128));

        assertThat(passwordHash.value())
                .startsWith("$pbkdf2-sha256$" + Pbkdf2PasswordHasher.ITERATIONS + "$")
                .hasSizeLessThanOrEqualTo(255);
    }
}
