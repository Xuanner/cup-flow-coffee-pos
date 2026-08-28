package com.cupflow.pos.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<Account> findByUsername(AccountUsername username);

    Optional<Account> findById(UUID accountId);

    boolean insertIfAbsent(Account account);

    boolean assignRoleIfAbsent(UUID accountId, RoleCode roleCode);
}
