package com.cupflow.pos.auth.domain;

public interface PasswordHasher {

    PasswordHash hash(CharSequence rawPassword);
}
