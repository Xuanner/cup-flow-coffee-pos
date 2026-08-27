package com.cupflow.pos.auth.domain;

import java.util.Objects;

public record AccountUsername(String value) {

    private static final int MAX_LENGTH = 64;

    public AccountUsername {
        Objects.requireNonNull(value, "value must not be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("username must contain between 1 and 64 characters");
        }
    }
}
