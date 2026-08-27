package com.cupflow.pos.auth.domain;

import java.util.Objects;

public final class PasswordHash {

    private static final int MAX_LENGTH = 255;
    private static final String REDACTED = "[REDACTED]";

    private final String value;

    private PasswordHash(String value) {
        this.value = value;
    }

    public static PasswordHash of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("password hash must contain between 1 and 255 characters");
        }
        return new PasswordHash(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object candidate) {
        return candidate instanceof PasswordHash other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return REDACTED;
    }
}
