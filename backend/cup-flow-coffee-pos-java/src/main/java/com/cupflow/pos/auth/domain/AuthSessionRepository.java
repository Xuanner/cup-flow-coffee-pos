package com.cupflow.pos.auth.domain;

import java.time.Instant;

public interface AuthSessionRepository {

    void insert(AuthSession session);

    void revokeByTokenHash(String tokenHash, Instant revokedAt, String reason);
}
