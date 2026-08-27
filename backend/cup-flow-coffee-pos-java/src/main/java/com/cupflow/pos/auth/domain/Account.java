package com.cupflow.pos.auth.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Account(
        UUID id,
        AccountUsername username,
        PasswordHash passwordHash,
        String displayName,
        AccountStatus status,
        Set<RoleCode> roles) {

    private static final int MAX_DISPLAY_NAME_LENGTH = 64;

    public Account {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        if (displayName.isBlank() || displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("display name must contain between 1 and 64 characters");
        }
        roles = Set.copyOf(roles);
    }

    public static Account newAccount(
            UUID id, AccountUsername username, PasswordHash passwordHash, String displayName, AccountStatus status) {
        return new Account(id, username, passwordHash, displayName, status, Set.of());
    }
}
