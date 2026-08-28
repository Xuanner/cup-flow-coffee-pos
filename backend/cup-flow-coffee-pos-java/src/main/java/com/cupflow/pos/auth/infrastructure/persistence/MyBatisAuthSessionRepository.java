package com.cupflow.pos.auth.infrastructure.persistence;

import com.cupflow.pos.auth.domain.AuthSession;
import com.cupflow.pos.auth.domain.AuthSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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
    public Optional<AuthSession> findActiveByTokenHash(String tokenHash) {
        AuthSessionPersistenceMapper.SessionRow row = mapper.findActiveByTokenHash(tokenHash);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new AuthSession(
                UUID.fromString(row.id()),
                UUID.fromString(row.accountId()),
                row.tokenHash(),
                row.createdAt(),
                row.lastActivityAt(),
                row.idleExpiresAt(),
                row.absoluteExpiresAt()));
    }

    @Override
    public boolean refreshActivity(String tokenHash, Instant acceptedAt, Instant idleExpiresAt) {
        return mapper.refreshActivity(tokenHash, acceptedAt, idleExpiresAt) == 1;
    }

    @Override
    public void revokeByTokenHash(String tokenHash, Instant revokedAt, String reason) {
        mapper.revokeByTokenHash(tokenHash, revokedAt, reason);
    }
}
