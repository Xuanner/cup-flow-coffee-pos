package com.cupflow.pos.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.PasswordHash;
import com.cupflow.pos.auth.domain.PasswordHasher;
import com.cupflow.pos.auth.domain.RoleCode;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountPasswordVerifierTest {

    @Test
    @DisplayName("TC-S2-PASS-004 未知账号执行等价密码校验且认证失败")
    void tcS2Pass004UnknownAccountExecutesTheSameHashVerificationAndStillFails() {
        PasswordHash decoyHash = PasswordHash.of("test-only-decoy-hash");
        RecordingPasswordHasher passwordHasher = new RecordingPasswordHasher(decoyHash, true);
        AccountPasswordVerifier verifier = new AccountPasswordVerifier(passwordHasher);

        boolean verified = verifier.verify("test-only-attempt", Optional.empty());

        assertThat(verified).isFalse();
        assertThat(passwordHasher.matchCalls).isEqualTo(1);
        assertThat(passwordHasher.lastCandidateHash).isEqualTo(decoyHash);
    }

    @Test
    void knownAccountUsesItsStoredHashAndReturnsTheHasherResult() {
        PasswordHash decoyHash = PasswordHash.of("test-only-decoy-hash");
        PasswordHash storedHash = PasswordHash.of("test-only-stored-hash");
        RecordingPasswordHasher passwordHasher = new RecordingPasswordHasher(decoyHash, true);
        AccountPasswordVerifier verifier = new AccountPasswordVerifier(passwordHasher);
        Account account = new Account(
                UUID.randomUUID(),
                new AccountUsername("cashier"),
                storedHash,
                "测试收银员",
                AccountStatus.ACTIVE,
                Set.of(RoleCode.CASHIER));

        assertThat(verifier.verify("test-only-attempt", Optional.of(account))).isTrue();
        assertThat(passwordHasher.matchCalls).isEqualTo(1);
        assertThat(passwordHasher.lastCandidateHash).isEqualTo(storedHash);
    }

    private static final class RecordingPasswordHasher implements PasswordHasher {

        private final PasswordHash generatedHash;
        private final boolean matchResult;
        private int matchCalls;
        private PasswordHash lastCandidateHash;

        private RecordingPasswordHasher(PasswordHash generatedHash, boolean matchResult) {
            this.generatedHash = generatedHash;
            this.matchResult = matchResult;
        }

        @Override
        public PasswordHash hash(CharSequence rawPassword) {
            return generatedHash;
        }

        @Override
        public boolean matches(CharSequence rawPassword, PasswordHash passwordHash) {
            matchCalls++;
            lastCandidateHash = passwordHash;
            return matchResult;
        }
    }
}
