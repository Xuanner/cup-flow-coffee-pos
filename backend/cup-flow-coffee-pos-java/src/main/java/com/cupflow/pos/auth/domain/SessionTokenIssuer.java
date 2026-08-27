package com.cupflow.pos.auth.domain;

public interface SessionTokenIssuer {

    SessionCredential issue();

    String hash(CharSequence rawToken);
}
