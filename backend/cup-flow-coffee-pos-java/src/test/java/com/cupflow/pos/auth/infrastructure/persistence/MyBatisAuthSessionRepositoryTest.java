package com.cupflow.pos.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.cupflow.pos.TestcontainersConfiguration;
import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.AuthSession;
import com.cupflow.pos.auth.domain.AuthSessionRepository;
import com.cupflow.pos.auth.domain.PasswordHash;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MyBatisAuthSessionRepositoryTest {

    @Autowired
    private AuthSessionRepository sessionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("TASK-S2-SESSION-01-01 会话按摘要读取且乱序刷新不回退活动时间")
    void readsByHashAndDoesNotRegressOnOutOfOrderRefreshes() {
        UUID accountId = createAccount();
        Instant createdAt = Instant.parse("2026-08-28T01:00:00Z");
        String tokenHash = "a".repeat(64);
        sessionRepository.insert(new AuthSession(
                UUID.randomUUID(),
                accountId,
                tokenHash,
                createdAt,
                createdAt,
                createdAt.plusSeconds(1800),
                createdAt.plusSeconds(28800)));

        assertThat(sessionRepository.refreshActivity(
                        tokenHash, createdAt.plusSeconds(300), createdAt.plusSeconds(2100)))
                .isTrue();
        assertThat(sessionRepository.refreshActivity(
                        tokenHash, createdAt.plusSeconds(180), createdAt.plusSeconds(1980)))
                .isTrue();

        assertThat(sessionRepository.findActiveByTokenHash(tokenHash)).get().satisfies(session -> {
            assertThat(session.lastActivityAt()).isEqualTo(createdAt.plusSeconds(300));
            assertThat(session.idleExpiresAt()).isEqualTo(createdAt.plusSeconds(2100));
            assertThat(session.toString()).doesNotContain(tokenHash);
        });
    }

    @Test
    @DisplayName("TASK-S2-SESSION-01-01 撤销后会话不可读取或刷新")
    void revokedSessionCannotBeReadOrRefreshed() {
        UUID accountId = createAccount();
        Instant createdAt = Instant.parse("2026-08-28T02:00:00Z");
        String tokenHash = "b".repeat(64);
        sessionRepository.insert(new AuthSession(
                UUID.randomUUID(),
                accountId,
                tokenHash,
                createdAt,
                createdAt,
                createdAt.plusSeconds(1800),
                createdAt.plusSeconds(28800)));

        sessionRepository.revokeByTokenHash(tokenHash, createdAt.plusSeconds(60), "REPLACED");

        assertThat(sessionRepository.findActiveByTokenHash(tokenHash)).isEmpty();
        assertThat(sessionRepository.refreshActivity(
                        tokenHash, createdAt.plusSeconds(120), createdAt.plusSeconds(1920)))
                .isFalse();
    }

    @Test
    @DisplayName("TC-S2-SESS-016 只删除失效满 7 天的撤销或过期会话")
    void deletesOnlyInvalidSessionsBeyondRetention() {
        UUID accountId = createAccount();
        Instant cutoff = Instant.parse("2026-08-21T00:00:00Z");
        insertSession(accountId, "c".repeat(64), cutoff.minusSeconds(3600), cutoff.minusSeconds(1800));
        insertSession(accountId, "d".repeat(64), cutoff.minusSeconds(60), cutoff.plusSeconds(60));
        insertSession(accountId, "e".repeat(64), cutoff.plusSeconds(3600), cutoff.plusSeconds(5400));
        insertSession(accountId, "f".repeat(64), cutoff.minusSeconds(3600), cutoff.minusSeconds(1));
        sessionRepository.revokeByTokenHash("c".repeat(64), cutoff.minusSeconds(1), "LOGOUT");
        sessionRepository.revokeByTokenHash("d".repeat(64), cutoff.plusSeconds(1), "LOGOUT");

        assertThat(sessionRepository.deleteInvalidBefore(cutoff)).isEqualTo(2);
        assertThat(sessionRepository.findActiveByTokenHash("c".repeat(64))).isEmpty();
        assertThat(sessionRepository.findActiveByTokenHash("d".repeat(64))).isEmpty();
        assertThat(sessionRepository.findActiveByTokenHash("e".repeat(64))).isPresent();
        assertThat(sessionRepository.findActiveByTokenHash("f".repeat(64))).isEmpty();
    }

    private void insertSession(UUID accountId, String tokenHash, Instant createdAt, Instant idleExpiresAt) {
        sessionRepository.insert(new AuthSession(
                UUID.randomUUID(),
                accountId,
                tokenHash,
                createdAt,
                createdAt,
                idleExpiresAt,
                createdAt.plusSeconds(28_800)));
    }

    private UUID createAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.newAccount(
                accountId,
                new AccountUsername("session-repository-" + accountId),
                PasswordHash.of("test-only-session-repository-hash"),
                "会话仓储测试账号",
                AccountStatus.ACTIVE);
        assertThat(accountRepository.insertIfAbsent(account)).isTrue();
        return accountId;
    }
}
