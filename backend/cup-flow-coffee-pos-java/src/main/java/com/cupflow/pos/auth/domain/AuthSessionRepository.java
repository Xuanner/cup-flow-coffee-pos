package com.cupflow.pos.auth.domain;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository {

    void insert(AuthSession session);

    Optional<AuthSession> findActiveByTokenHash(String tokenHash);

    boolean refreshActivity(String tokenHash, Instant acceptedAt, Instant idleExpiresAt);

    void revokeByTokenHash(String tokenHash, Instant revokedAt, String reason);
}
