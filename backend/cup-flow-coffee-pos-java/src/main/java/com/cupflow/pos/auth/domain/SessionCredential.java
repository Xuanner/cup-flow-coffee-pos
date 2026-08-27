package com.cupflow.pos.auth.domain;

import java.util.Objects;

public final class SessionCredential {

    private final String rawValue;
    private final String hash;

    public SessionCredential(String rawValue, String hash) {
        this.rawValue = Objects.requireNonNull(rawValue, "rawValue must not be null");
        this.hash = Objects.requireNonNull(hash, "hash must not be null");
    }

    public String rawValue() {
        return rawValue;
    }

    public String hash() {
        return hash;
    }

    @Override
    public String toString() {
        return "[REDACTED]";
    }
}
