package com.cupflow.pos.auth.infrastructure.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CsrfTokenService {

    public static final String HEADER_NAME = "X-XSRF-TOKEN";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Duration FUTURE_SKEW = Duration.ofMinutes(1);
    private static final int NONCE_BYTES = 32;

    private final byte[] signingKey = new byte[32];
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public CsrfTokenService(Clock clock) {
        this(clock, new SecureRandom());
    }

    CsrfTokenService(Clock clock, SecureRandom secureRandom) {
        this.clock = clock;
        this.secureRandom = secureRandom;
        secureRandom.nextBytes(signingKey);
    }

    public String issue() {
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        byte[] payload = ByteBuffer.allocate(Long.BYTES + nonce.length)
                .putLong(clock.instant().getEpochSecond())
                .put(nonce)
                .array();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String encodedPayload = encoder.encodeToString(payload);
        return encodedPayload + "." + encoder.encodeToString(sign(encodedPayload));
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2) {
                return false;
            }
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[1]);
            byte[] expectedSignature = sign(parts[0]);
            if (!MessageDigest.isEqual(suppliedSignature, expectedSignature)) {
                return false;
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            if (payload.length != Long.BYTES + NONCE_BYTES) {
                return false;
            }
            Instant issuedAt = Instant.ofEpochSecond(ByteBuffer.wrap(payload).getLong());
            Instant now = clock.instant();
            return !issuedAt.isAfter(now.plus(FUTURE_SKEW))
                    && issuedAt.plus(TOKEN_LIFETIME).isAfter(now);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("CSRF protection is unavailable", exception);
        }
    }
}
