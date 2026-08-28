package com.cupflow.pos.auth.infrastructure.persistence;

import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.PasswordHash;
import com.cupflow.pos.auth.domain.RoleCode;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAccountRepository implements AccountRepository {

    private final AccountPersistenceMapper mapper;

    public MyBatisAccountRepository(AccountPersistenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Account> findByUsername(AccountUsername username) {
        return toAccount(mapper.findByUsername(username.value()));
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return toAccount(mapper.findById(accountId.toString()));
    }

    private Optional<Account> toAccount(AccountPersistenceMapper.AccountRow row) {
        if (row == null) {
            return Optional.empty();
        }

        Set<RoleCode> roles =
                mapper.findRoleCodes(row.id()).stream().map(RoleCode::valueOf).collect(Collectors.toUnmodifiableSet());
        return Optional.of(new Account(
                UUID.fromString(row.id()),
                new AccountUsername(row.username()),
                PasswordHash.of(row.passwordHash()),
                row.displayName(),
                AccountStatus.valueOf(row.status()),
                roles));
    }

    @Override
    public boolean insertIfAbsent(Account account) {
        return mapper.insertIfAbsent(
                        account.id().toString(),
                        account.username().value(),
                        account.passwordHash().value(),
                        account.displayName(),
                        account.status().name())
                == 1;
    }

    @Override
    public boolean assignRoleIfAbsent(UUID accountId, RoleCode roleCode) {
        if (!mapper.roleExists(roleCode.name())) {
            throw new IllegalStateException("Configured role does not exist");
        }
        return mapper.assignRoleIfAbsent(accountId.toString(), roleCode.name()) == 1;
    }
}
