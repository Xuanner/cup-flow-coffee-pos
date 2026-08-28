package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.AuthSessionRepository;
import com.cupflow.pos.auth.domain.SessionTokenIssuer;
import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutService {

    private static final Pattern SESSION_TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final AuthSessionRepository sessionRepository;
    private final SessionTokenIssuer tokenIssuer;
    private final Clock clock;

    public LogoutService(AuthSessionRepository sessionRepository, SessionTokenIssuer tokenIssuer, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Transactional
    public void logout(String rawSessionToken) {
        if (rawSessionToken == null
                || !SESSION_TOKEN_FORMAT.matcher(rawSessionToken).matches()) {
            return;
        }
        sessionRepository.revokeByTokenHash(tokenIssuer.hash(rawSessionToken), clock.instant(), "LOGOUT");
    }
}
