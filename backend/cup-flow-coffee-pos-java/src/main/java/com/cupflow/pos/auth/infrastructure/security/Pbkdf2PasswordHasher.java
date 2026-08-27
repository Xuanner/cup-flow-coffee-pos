package com.cupflow.pos.auth.infrastructure.security;

import com.cupflow.pos.auth.domain.PasswordHash;
import com.cupflow.pos.auth.domain.PasswordHasher;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
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
    private static final int HASH_BYTES = HASH_BITS / Byte.SIZE;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT_ALGORITHM = "pbkdf2-sha256";
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
        if (rawPassword.isEmpty() || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("raw password length is invalid");
        }
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        char[] password = copy(rawPassword);
        byte[] derivedKey = null;
        try {
            derivedKey = derive(password, salt, ITERATIONS);
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return PasswordHash.of(
                    FORMAT.formatted(ITERATIONS, encoder.encodeToString(salt), encoder.encodeToString(derivedKey)));
        } finally {
            Arrays.fill(password, '\0');
            if (derivedKey != null) {
                Arrays.fill(derivedKey, (byte) 0);
            }
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, PasswordHash passwordHash) {
        if (rawPassword == null
                || rawPassword.isEmpty()
                || rawPassword.length() > MAX_PASSWORD_LENGTH
                || passwordHash == null) {
            return false;
        }

        ParsedHash parsedHash = parse(passwordHash.value());
        if (parsedHash == null) {
            return false;
        }

        char[] password = copy(rawPassword);
        byte[] actualHash = null;
        try {
            actualHash = derive(password, parsedHash.salt(), parsedHash.iterations());
            return MessageDigest.isEqual(parsedHash.expectedHash(), actualHash);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(parsedHash.salt(), (byte) 0);
            Arrays.fill(parsedHash.expectedHash(), (byte) 0);
            if (actualHash != null) {
                Arrays.fill(actualHash, (byte) 0);
            }
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            keySpec.clearPassword();
        }
    }

    private static ParsedHash parse(String encodedHash) {
        try {
            String[] fields = encodedHash.split("\\$", -1);
            int iterations = fields.length == 5 ? Integer.parseInt(fields[2]) : -1;
            if (fields.length != 5
                    || !fields[0].isEmpty()
                    || !FORMAT_ALGORITHM.equals(fields[1])
                    || iterations != ITERATIONS) {
                return null;
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] salt = decoder.decode(fields[3]);
            byte[] expectedHash = decoder.decode(fields[4]);
            if (salt.length != SALT_BYTES || expectedHash.length != HASH_BYTES) {
                Arrays.fill(salt, (byte) 0);
                Arrays.fill(expectedHash, (byte) 0);
                return null;
            }
            return new ParsedHash(iterations, salt, expectedHash);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static char[] copy(CharSequence source) {
        char[] copy = new char[source.length()];
        for (int index = 0; index < source.length(); index++) {
            copy[index] = source.charAt(index);
        }
        return copy;
    }

    private record ParsedHash(int iterations, byte[] salt, byte[] expectedHash) {}
}
