package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.PasswordHasher;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountBootstrapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountBootstrapService.class);

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;

    public AccountBootstrapService(AccountRepository accountRepository, PasswordHasher passwordHasher) {
        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public BootstrapResult initialize(BootstrapAccount cashier, BootstrapAccount admin) {
        if (cashier.username().equals(admin.username())) {
            throw new IllegalStateException("Bootstrap usernames must be different");
        }

        int created = initializeAccount(cashier);
        created += initializeAccount(admin);
        return new BootstrapResult(created, 2 - created);
    }

    private int initializeAccount(BootstrapAccount bootstrapAccount) {
        if (accountRepository.findByUsername(bootstrapAccount.username()).isPresent()) {
            LOGGER.info(
                    "Authentication bootstrap skipped an existing account for role {}", bootstrapAccount.roleCode());
            return 0;
        }

        Account account = Account.newAccount(
                UUID.randomUUID(),
                bootstrapAccount.username(),
                passwordHasher.hash(bootstrapAccount.password()),
                bootstrapAccount.displayName(),
                AccountStatus.ACTIVE);
        if (!accountRepository.insertIfAbsent(account)) {
            LOGGER.info(
                    "Authentication bootstrap skipped a concurrently created account for role {}",
                    bootstrapAccount.roleCode());
            return 0;
        }
        if (!accountRepository.assignRoleIfAbsent(account.id(), bootstrapAccount.roleCode())) {
            throw new IllegalStateException("Bootstrap role assignment failed");
        }

        LOGGER.info("Authentication bootstrap created an account for role {}", bootstrapAccount.roleCode());
        return 1;
    }

    public record BootstrapResult(int createdCount, int skippedCount) {}
}
