package com.cupflow.pos.auth.infrastructure.persistence;

import com.cupflow.pos.auth.domain.AuthSession;
import com.cupflow.pos.auth.domain.AuthSessionRepository;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAuthSessionRepository implements AuthSessionRepository {

    private final AuthSessionPersistenceMapper mapper;

    public MyBatisAuthSessionRepository(AuthSessionPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(AuthSession session) {
        int inserted = mapper.insert(
                session.id().toString(),
                session.accountId().toString(),
                session.tokenHash(),
                session.createdAt(),
                session.lastActivityAt(),
                session.idleExpiresAt(),
                session.absoluteExpiresAt());
        if (inserted != 1) {
            throw new IllegalStateException("Authentication session creation failed");
        }
    }

    @Override
    public void revokeByTokenHash(String tokenHash, Instant revokedAt, String reason) {
        mapper.revokeByTokenHash(tokenHash, revokedAt, reason);
    }
}
