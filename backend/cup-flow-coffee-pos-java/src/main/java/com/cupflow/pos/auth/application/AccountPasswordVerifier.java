package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.PasswordHash;
import com.cupflow.pos.auth.domain.PasswordHasher;
import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AccountPasswordVerifier {

    private static final int DECOY_PASSWORD_LENGTH = 32;
    private static final char[] DECOY_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

    private final PasswordHasher passwordHasher;
    private final PasswordHash decoyPasswordHash;

    public AccountPasswordVerifier(PasswordHasher passwordHasher) {
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
        SecureRandom secureRandom = new SecureRandom();
        char[] decoyPassword = new char[DECOY_PASSWORD_LENGTH];
        for (int index = 0; index < decoyPassword.length; index++) {
            decoyPassword[index] = DECOY_ALPHABET[secureRandom.nextInt(DECOY_ALPHABET.length)];
        }
        try {
            this.decoyPasswordHash = passwordHasher.hash(CharBuffer.wrap(decoyPassword));
        } finally {
            Arrays.fill(decoyPassword, '\0');
        }
    }

    public boolean verify(CharSequence rawPassword, Optional<Account> account) {
        Objects.requireNonNull(account, "account must not be null");
        PasswordHash candidateHash = account.map(Account::passwordHash).orElse(decoyPasswordHash);
        boolean passwordMatches = passwordHasher.matches(rawPassword, candidateHash);
        return account.isPresent() && passwordMatches;
    }
}
