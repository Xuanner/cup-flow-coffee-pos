package com.cupflow.pos.auth.infrastructure.security;

import com.cupflow.pos.auth.domain.SessionCredential;
import com.cupflow.pos.auth.domain.SessionTokenIssuer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SecureSessionTokenIssuer implements SessionTokenIssuer {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;

    public SecureSessionTokenIssuer() {
        this(new SecureRandom());
    }

    SecureSessionTokenIssuer(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    @Override
    public SessionCredential issue() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);
        String rawValue = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        return new SessionCredential(rawValue, hash(rawValue));
    }

    @Override
    public String hash(CharSequence rawToken) {
        Objects.requireNonNull(rawToken, "rawToken must not be null");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Session token hashing is unavailable", exception);
        }
    }
}
