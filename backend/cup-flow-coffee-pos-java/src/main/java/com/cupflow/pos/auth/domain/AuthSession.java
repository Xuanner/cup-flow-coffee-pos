package com.cupflow.pos.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthSession(
        UUID id,
        UUID accountId,
        String tokenHash,
        Instant createdAt,
        Instant lastActivityAt,
        Instant idleExpiresAt,
        Instant absoluteExpiresAt) {

    public AuthSession {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(lastActivityAt, "lastActivityAt must not be null");
        Objects.requireNonNull(idleExpiresAt, "idleExpiresAt must not be null");
        Objects.requireNonNull(absoluteExpiresAt, "absoluteExpiresAt must not be null");
        if (!tokenHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("session token hash must be a lowercase SHA-256 value");
        }
        if (!idleExpiresAt.isAfter(lastActivityAt) || !absoluteExpiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("session expiry must be after creation and activity");
        }
    }

    @Override
    public String toString() {
        return "AuthSession[id=" + id + ", accountId=" + accountId + ", tokenHash=[REDACTED]]";
    }
}
