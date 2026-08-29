package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.AuthSessionRepository;
import com.cupflow.pos.auth.domain.SessionTokenIssuer;
import com.cupflow.pos.shared.logging.SecurityEventOutcome;
import com.cupflow.pos.shared.logging.SecurityEventRecorder;
import com.cupflow.pos.shared.logging.SecurityEventType;
import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutService {

    private static final Pattern SESSION_TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final AuthSessionRepository sessionRepository;
    private final SessionTokenIssuer tokenIssuer;
    private final SecurityEventRecorder securityEventRecorder;
    private final Clock clock;

    public LogoutService(
            AuthSessionRepository sessionRepository,
            SessionTokenIssuer tokenIssuer,
            SecurityEventRecorder securityEventRecorder,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.tokenIssuer = tokenIssuer;
        this.securityEventRecorder = securityEventRecorder;
        this.clock = clock;
    }

    @Transactional
    public void logout(String rawSessionToken) {
        if (rawSessionToken == null
                || !SESSION_TOKEN_FORMAT.matcher(rawSessionToken).matches()) {
            record(null);
            return;
        }
        String tokenHash = tokenIssuer.hash(rawSessionToken);
        java.util.UUID accountId = sessionRepository
                .findActiveByTokenHash(tokenHash)
                .map(com.cupflow.pos.auth.domain.AuthSession::accountId)
                .orElse(null);
        sessionRepository.revokeByTokenHash(tokenHash, clock.instant(), "LOGOUT");
        record(accountId);
    }

    private void record(java.util.UUID accountId) {
        securityEventRecorder.record(
                SecurityEventType.LOGOUT_SUCCEEDED, SecurityEventOutcome.SUCCEEDED, accountId, "auth.logout", null);
    }
}
