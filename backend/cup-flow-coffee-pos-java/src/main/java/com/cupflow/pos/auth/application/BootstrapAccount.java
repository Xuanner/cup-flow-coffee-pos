package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.RoleCode;
import java.util.Objects;

public final class BootstrapAccount {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MAX_DISPLAY_NAME_LENGTH = 64;

    private final AccountUsername username;
    private final String password;
    private final String displayName;
    private final RoleCode roleCode;

    private BootstrapAccount(AccountUsername username, String password, String displayName, RoleCode roleCode) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.roleCode = roleCode;
    }

    public static BootstrapAccount of(String username, String password, String displayName, RoleCode roleCode) {
        Objects.requireNonNull(roleCode, "roleCode must not be null");
        AccountUsername normalizedUsername;
        try {
            normalizedUsername = new AccountUsername(username);
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new IllegalStateException("Bootstrap username must contain between 1 and 64 characters", exception);
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalStateException("Bootstrap password must contain between 12 and 128 characters");
        }
        if (displayName == null || displayName.isBlank() || displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalStateException("Bootstrap display name must contain between 1 and 64 characters");
        }
        return new BootstrapAccount(normalizedUsername, password, displayName, roleCode);
    }

    public AccountUsername username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String displayName() {
        return displayName;
    }

    public RoleCode roleCode() {
        return roleCode;
    }

    @Override
    public String toString() {
        return "BootstrapAccount[username=[REDACTED], password=[REDACTED], displayName=[REDACTED], roleCode="
                + roleCode
                + "]";
    }
}
