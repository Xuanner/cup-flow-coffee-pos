package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AuthSession;
import com.cupflow.pos.auth.domain.AuthSessionRepository;
import com.cupflow.pos.auth.domain.SessionTokenIssuer;
import com.cupflow.pos.shared.logging.SecurityEventOutcome;
import com.cupflow.pos.shared.logging.SecurityEventRecorder;
import com.cupflow.pos.shared.logging.SecurityEventType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentSessionService {

    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);
    private static final Pattern SESSION_TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final AuthSessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final SessionTokenIssuer tokenIssuer;
    private final SecurityEventRecorder securityEventRecorder;
    private final Clock clock;

    public CurrentSessionService(
            AuthSessionRepository sessionRepository,
            AccountRepository accountRepository,
            SessionTokenIssuer tokenIssuer,
            SecurityEventRecorder securityEventRecorder,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.tokenIssuer = tokenIssuer;
        this.securityEventRecorder = securityEventRecorder;
        this.clock = clock;
    }

    @Transactional
    public CurrentSessionResult resolve(String rawSessionToken) {
        if (rawSessionToken == null
                || !SESSION_TOKEN_FORMAT.matcher(rawSessionToken).matches()) {
            invalidated(null, "INVALID_CREDENTIAL");
            return new CurrentSessionResult.Invalid();
        }

        String tokenHash = tokenIssuer.hash(rawSessionToken);
        Optional<AuthSession> candidate = sessionRepository.findActiveByTokenHash(tokenHash);
        if (candidate.isEmpty()) {
            invalidated(null, "INVALID_CREDENTIAL");
            return new CurrentSessionResult.Invalid();
        }

        AuthSession session = candidate.orElseThrow();
        Instant now = clock.instant();
        if (!now.isBefore(session.absoluteExpiresAt())) {
            sessionRepository.revokeByTokenHash(tokenHash, now, "ABSOLUTE_TIMEOUT");
            securityEventRecorder.record(
                    SecurityEventType.SESSION_EXPIRED,
                    SecurityEventOutcome.INVALIDATED,
                    session.accountId(),
                    "auth.session",
                    "ABSOLUTE_TIMEOUT");
            return new CurrentSessionResult.Invalid();
        }
        if (!now.isBefore(session.idleExpiresAt())) {
            sessionRepository.revokeByTokenHash(tokenHash, now, "IDLE_TIMEOUT");
            securityEventRecorder.record(
                    SecurityEventType.SESSION_EXPIRED,
                    SecurityEventOutcome.INVALIDATED,
                    session.accountId(),
                    "auth.session",
                    "IDLE_TIMEOUT");
            return new CurrentSessionResult.Invalid();
        }

        Optional<Account> accountCandidate = accountRepository.findById(session.accountId());
        if (accountCandidate.isEmpty()
                || accountCandidate.orElseThrow().status() != AccountStatus.ACTIVE
                || accountCandidate.orElseThrow().roles().isEmpty()) {
            sessionRepository.revokeByTokenHash(tokenHash, now, "ACCOUNT_DISABLED");
            securityEventRecorder.record(
                    SecurityEventType.ACCOUNT_SESSION_INVALIDATED,
                    SecurityEventOutcome.INVALIDATED,
                    session.accountId(),
                    "auth.session",
                    "ACCOUNT_UNAVAILABLE");
            return new CurrentSessionResult.Invalid();
        }

        Instant refreshedIdleExpiry = min(now.plus(IDLE_TIMEOUT), session.absoluteExpiresAt());
        if (!sessionRepository.refreshActivity(tokenHash, now, refreshedIdleExpiry)) {
            invalidated(session.accountId(), "CONCURRENT_CHANGE");
            return new CurrentSessionResult.Invalid();
        }
        return new CurrentSessionResult.Authenticated(CurrentUser.from(accountCandidate.orElseThrow()));
    }

    private Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private void invalidated(java.util.UUID accountId, String reason) {
        securityEventRecorder.record(
                SecurityEventType.SESSION_INVALIDATED,
                SecurityEventOutcome.INVALIDATED,
                accountId,
                "auth.session",
                reason);
    }
}
