package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.AuthSession;
import com.cupflow.pos.auth.domain.AuthSessionRepository;
import com.cupflow.pos.auth.domain.SessionCredential;
import com.cupflow.pos.auth.domain.SessionTokenIssuer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration ABSOLUTE_TIMEOUT = Duration.ofHours(8);

    private final AccountRepository accountRepository;
    private final AccountPasswordVerifier passwordVerifier;
    private final AuthSessionRepository sessionRepository;
    private final SessionTokenIssuer tokenIssuer;
    private final Clock clock;

    public LoginService(
            AccountRepository accountRepository,
            AccountPasswordVerifier passwordVerifier,
            AuthSessionRepository sessionRepository,
            SessionTokenIssuer tokenIssuer,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.passwordVerifier = passwordVerifier;
        this.sessionRepository = sessionRepository;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Transactional
    public LoginResult login(String username, String password, String previousSessionToken) {
        AccountUsername normalizedUsername = new AccountUsername(username);
        Optional<Account> candidate = accountRepository.findByUsername(normalizedUsername);
        boolean passwordMatches = passwordVerifier.verify(password, candidate);
        if (candidate.isEmpty()
                || !passwordMatches
                || candidate.orElseThrow().status() != AccountStatus.ACTIVE
                || candidate.orElseThrow().roles().isEmpty()) {
            return new LoginResult.Failure();
        }

        Instant now = clock.instant();
        if (previousSessionToken != null && !previousSessionToken.isBlank()) {
            sessionRepository.revokeByTokenHash(tokenIssuer.hash(previousSessionToken), now, "REPLACED");
        }

        Account account = candidate.orElseThrow();
        SessionCredential credential = tokenIssuer.issue();
        sessionRepository.insert(new AuthSession(
                UUID.randomUUID(),
                account.id(),
                credential.hash(),
                now,
                now,
                now.plus(IDLE_TIMEOUT),
                now.plus(ABSOLUTE_TIMEOUT)));
        return new LoginResult.Success(CurrentUser.from(account), credential.rawValue());
    }
}
