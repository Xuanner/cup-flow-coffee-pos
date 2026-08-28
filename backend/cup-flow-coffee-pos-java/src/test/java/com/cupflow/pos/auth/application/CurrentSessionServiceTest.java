package com.cupflow.pos.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.AuthSession;
import com.cupflow.pos.auth.domain.AuthSessionRepository;
import com.cupflow.pos.auth.domain.PasswordHash;
import com.cupflow.pos.auth.domain.RoleCode;
import com.cupflow.pos.auth.domain.SessionTokenIssuer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CurrentSessionServiceTest {

    private static final String TOKEN = "A".repeat(43);
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-08-28T04:00:00Z");

    private final AuthSessionRepository sessionRepository = mock(AuthSessionRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final SessionTokenIssuer tokenIssuer = mock(SessionTokenIssuer.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final UUID accountId = UUID.randomUUID();
    private final CurrentSessionService service =
            new CurrentSessionService(sessionRepository, accountRepository, tokenIssuer, clock);

    @BeforeEach
    void configureToken() {
        when(tokenIssuer.hash(TOKEN)).thenReturn(TOKEN_HASH);
    }

    @Test
    @DisplayName("TC-S2-SESS-004/006 空闲和绝对边界前会话有效且刷新不超过绝对时限")
    void acceptsSessionBeforeBothBoundaries() {
        AuthSession session =
                session(NOW.minusSeconds(28_799), NOW.minusSeconds(60), NOW.plusSeconds(1), NOW.plusSeconds(1));
        when(sessionRepository.findActiveByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
        when(sessionRepository.refreshActivity(TOKEN_HASH, NOW, NOW.plusSeconds(1)))
                .thenReturn(true);

        assertThat(service.resolve(TOKEN)).isInstanceOf(CurrentSessionResult.Authenticated.class);
        verify(sessionRepository).refreshActivity(TOKEN_HASH, NOW, NOW.plusSeconds(1));
        verify(sessionRepository, never()).revokeByTokenHash(TOKEN_HASH, NOW, "IDLE_TIMEOUT");
    }

    @Test
    @DisplayName("TC-S2-SESS-005 达到空闲 30 分钟边界立即撤销")
    void rejectsAtIdleBoundary() {
        AuthSession session = session(NOW.minusSeconds(3600), NOW.minusSeconds(1800), NOW, NOW.plusSeconds(3600));
        when(sessionRepository.findActiveByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        assertThat(service.resolve(TOKEN)).isInstanceOf(CurrentSessionResult.Invalid.class);
        verify(sessionRepository).revokeByTokenHash(TOKEN_HASH, NOW, "IDLE_TIMEOUT");
        verify(accountRepository, never()).findById(accountId);
    }

    @Test
    @DisplayName("TC-S2-SESS-007 达到绝对 8 小时边界立即撤销")
    void rejectsAtAbsoluteBoundary() {
        AuthSession session = session(NOW.minusSeconds(28_800), NOW.minusSeconds(1800), NOW, NOW);
        when(sessionRepository.findActiveByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        assertThat(service.resolve(TOKEN)).isInstanceOf(CurrentSessionResult.Invalid.class);
        verify(sessionRepository).revokeByTokenHash(TOKEN_HASH, NOW, "ABSOLUTE_TIMEOUT");
        verify(accountRepository, never()).findById(accountId);
    }

    @Test
    @DisplayName("TC-S2-SESS-008 会话期间账号停用立即撤销")
    void rejectsDisabledAccount() {
        AuthSession session =
                session(NOW.minusSeconds(60), NOW.minusSeconds(30), NOW.plusSeconds(1770), NOW.plusSeconds(28_740));
        when(sessionRepository.findActiveByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(AccountStatus.DISABLED)));

        assertThat(service.resolve(TOKEN)).isInstanceOf(CurrentSessionResult.Invalid.class);
        verify(sessionRepository).revokeByTokenHash(TOKEN_HASH, NOW, "ACCOUNT_DISABLED");
        verify(sessionRepository, never()).refreshActivity(TOKEN_HASH, NOW, NOW.plusSeconds(1800));
    }

    private AuthSession session(
            Instant createdAt, Instant lastActivityAt, Instant idleExpiresAt, Instant absoluteExpiresAt) {
        return new AuthSession(
                UUID.randomUUID(), accountId, TOKEN_HASH, createdAt, lastActivityAt, idleExpiresAt, absoluteExpiresAt);
    }

    private Account account(AccountStatus status) {
        return new Account(
                accountId,
                new AccountUsername("session-boundary"),
                PasswordHash.of("test-only-session-boundary-hash"),
                "会话边界账号",
                status,
                Set.of(RoleCode.CASHIER));
    }
}
