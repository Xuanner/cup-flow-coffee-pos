package com.cupflow.pos.auth.domain;

public interface PasswordHasher {

    PasswordHash hash(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, PasswordHash passwordHash);
}
