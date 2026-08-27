package com.cupflow.pos.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cupflow.pos.TestcontainersConfiguration;
import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.PasswordHash;
import com.cupflow.pos.auth.domain.RoleCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MyBatisAccountRepositoryTest {

    @Autowired
    private AccountRepository repository;

    @Test
    void createsAndLoadsAnActiveAccountWithMultipleRoles() {
        Account account = newAccount("  multi-role-" + UUID.randomUUID() + "  ", AccountStatus.ACTIVE);

        assertThat(repository.insertIfAbsent(account)).isTrue();
        assertThat(repository.assignRoleIfAbsent(account.id(), RoleCode.CASHIER))
                .isTrue();
        assertThat(repository.assignRoleIfAbsent(account.id(), RoleCode.ADMIN)).isTrue();

        Account loaded = repository.findByUsername(account.username()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(account.id());
        assertThat(loaded.username()).isEqualTo(account.username());
        assertThat(loaded.passwordHash()).isEqualTo(account.passwordHash());
        assertThat(loaded.displayName()).isEqualTo(account.displayName());
        assertThat(loaded.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(loaded.roles()).containsExactlyInAnyOrder(RoleCode.CASHIER, RoleCode.ADMIN);
    }

    @Test
    void returnsEmptyForAnUnknownUsername() {
        assertThat(repository.findByUsername(new AccountUsername("missing-" + UUID.randomUUID())))
                .isEmpty();
    }

    @Test
    void preservesDisabledStatus() {
        Account account = newAccount("disabled-" + UUID.randomUUID(), AccountStatus.DISABLED);
        repository.insertIfAbsent(account);
        repository.assignRoleIfAbsent(account.id(), RoleCode.CASHIER);

        Account loaded = repository.findByUsername(account.username()).orElseThrow();

        assertThat(loaded.status()).isEqualTo(AccountStatus.DISABLED);
        assertThat(loaded.roles()).containsExactly(RoleCode.CASHIER);
    }

    @Test
    void repeatedWritesDoNotOverwriteAnExistingAccountOrDuplicateItsRole() {
        String username = "repeat-" + UUID.randomUUID();
        Account original = newAccount(username, AccountStatus.DISABLED);
        Account replacement = Account.newAccount(
                UUID.randomUUID(),
                new AccountUsername(username),
                PasswordHash.of("replacement-test-hash"),
                "替换名称",
                AccountStatus.ACTIVE);

        assertThat(repository.insertIfAbsent(original)).isTrue();
        assertThat(repository.assignRoleIfAbsent(original.id(), RoleCode.ADMIN)).isTrue();
        assertThat(repository.insertIfAbsent(replacement)).isFalse();
        assertThat(repository.assignRoleIfAbsent(original.id(), RoleCode.ADMIN)).isFalse();

        Account loaded =
                repository.findByUsername(new AccountUsername(username)).orElseThrow();
        assertThat(loaded.id()).isEqualTo(original.id());
        assertThat(loaded.passwordHash()).isEqualTo(original.passwordHash());
        assertThat(loaded.displayName()).isEqualTo(original.displayName());
        assertThat(loaded.status()).isEqualTo(AccountStatus.DISABLED);
        assertThat(loaded.roles()).containsExactly(RoleCode.ADMIN);
    }

    @Test
    void exactlyOneConcurrentInsertWinsForTheSameUsername() throws Exception {
        String username = "concurrent-" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Boolean>> attempts = new ArrayList<>();
            for (int index = 0; index < 8; index++) {
                attempts.add(() -> repository.insertIfAbsent(newAccount(username, AccountStatus.ACTIVE)));
            }

            List<Future<Boolean>> results = executor.invokeAll(attempts);

            assertThat(results)
                    .extracting(Future::get)
                    .containsExactlyInAnyOrderElementsOf(
                            List.of(true, false, false, false, false, false, false, false));
            assertThat(repository.findByUsername(new AccountUsername(username))).isPresent();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void roleAssignmentHonorsTheAccountForeignKeyConstraint() {
        assertThatThrownBy(() -> repository.assignRoleIfAbsent(UUID.randomUUID(), RoleCode.CASHIER))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Account newAccount(String username, AccountStatus status) {
        UUID id = UUID.randomUUID();
        return Account.newAccount(
                id, new AccountUsername(username), PasswordHash.of("test-only-hash-" + id), "测试账号", status);
    }
}
