package com.cupflow.pos.auth.infrastructure.security;

import com.cupflow.pos.auth.domain.PasswordHash;
import com.cupflow.pos.auth.domain.PasswordHasher;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class Pbkdf2PasswordHasher implements PasswordHasher {

    static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT = "$pbkdf2-sha256$%d$%s$%s";

    private final SecureRandom secureRandom;

    public Pbkdf2PasswordHasher() {
        this(new SecureRandom());
    }

    Pbkdf2PasswordHasher(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    @Override
    public PasswordHash hash(CharSequence rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        char[] password = rawPassword.toString().toCharArray();
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, HASH_BITS);
        try {
            byte[] derivedKey = SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(keySpec)
                    .getEncoded();
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return PasswordHash.of(
                    FORMAT.formatted(ITERATIONS, encoder.encodeToString(salt), encoder.encodeToString(derivedKey)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            keySpec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }
}
