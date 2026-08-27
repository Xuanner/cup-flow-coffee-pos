package com.cupflow.pos.auth.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cupflow.pos.TestcontainersConfiguration;
import com.cupflow.pos.auth.application.AccountBootstrapService;
import com.cupflow.pos.auth.application.BootstrapAccount;
import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.RoleCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "auth.bootstrap.enabled=true",
            "auth.bootstrap.cashier.username=bootstrap-cashier",
            "auth.bootstrap.cashier.password=test-only-cashier-password",
            "auth.bootstrap.cashier.display-name=初始化收银员",
            "auth.bootstrap.admin.username=bootstrap-admin",
            "auth.bootstrap.admin.password=test-only-admin-password",
            "auth.bootstrap.admin.display-name=初始化管理员"
        })
class AccountBootstrapIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountBootstrapService bootstrapService;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    @DisplayName("TC-S2-DATA-005 首次启用初始化会创建 ACTIVE 收银员和管理员并绑定正确角色")
    void createsTheMinimumActiveAccountSet() {
        Account cashier = accountRepository
                .findByUsername(new AccountUsername("bootstrap-cashier"))
                .orElseThrow();
        Account admin = accountRepository
                .findByUsername(new AccountUsername("bootstrap-admin"))
                .orElseThrow();

        assertThat(cashier.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(cashier.displayName()).isEqualTo("初始化收银员");
        assertThat(cashier.roles()).containsExactly(RoleCode.CASHIER);
        assertThat(cashier.passwordHash().value())
                .startsWith("$pbkdf2-sha256$600000$")
                .doesNotContain("test-only-cashier-password");
        assertThat(admin.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(admin.displayName()).isEqualTo("初始化管理员");
        assertThat(admin.roles()).containsExactly(RoleCode.ADMIN);
        assertThat(admin.passwordHash().value())
                .startsWith("$pbkdf2-sha256$600000$")
                .doesNotContain("test-only-admin-password");
    }

    @Test
    @DisplayName("TC-S2-DATA-006 重复初始化不覆盖已有密码、状态、展示名或角色")
    void repeatedInitializationDoesNotOverwriteExistingAccounts() {
        String suffix = UUID.randomUUID().toString();
        BootstrapAccount cashier =
                BootstrapAccount.of("repeat-cashier-" + suffix, "cashier-test-password", "原收银员", RoleCode.CASHIER);
        BootstrapAccount admin =
                BootstrapAccount.of("repeat-admin-" + suffix, "admin-test-password", "原管理员", RoleCode.ADMIN);
        bootstrapService.initialize(cashier, admin);
        Account original = accountRepository.findByUsername(cashier.username()).orElseThrow();
        String preservedHash = "test-only-preserved-password-hash";

        jdbcClient
                .sql("""
                        UPDATE accounts
                        SET password_hash = :passwordHash,
                            display_name = '人工修改名称',
                            status = 'DISABLED'
                        WHERE id = CAST(:accountId AS UUID)
                        """)
                .param("passwordHash", preservedHash)
                .param("accountId", original.id().toString())
                .update();
        jdbcClient
                .sql("DELETE FROM account_roles WHERE account_id = CAST(:accountId AS UUID)")
                .param("accountId", original.id().toString())
                .update();
        accountRepository.assignRoleIfAbsent(original.id(), RoleCode.ADMIN);

        AccountBootstrapService.BootstrapResult result = bootstrapService.initialize(cashier, admin);
        Account unchanged = accountRepository.findByUsername(cashier.username()).orElseThrow();

        assertThat(result.createdCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(2);
        assertThat(unchanged.id()).isEqualTo(original.id());
        assertThat(unchanged.passwordHash().value()).isEqualTo(preservedHash);
        assertThat(unchanged.displayName()).isEqualTo("人工修改名称");
        assertThat(unchanged.status()).isEqualTo(AccountStatus.DISABLED);
        assertThat(unchanged.roles()).containsExactly(RoleCode.ADMIN);
    }

    @Test
    @DisplayName("TC-S2-DATA-007 启用初始化但 Secret 缺失时在写库前失败关闭")
    void rejectsIncompleteConfigurationBeforeWritingAnyAccount() {
        String suffix = UUID.randomUUID().toString();
        AuthBootstrapProperties invalidProperties = new AuthBootstrapProperties();
        invalidProperties.setEnabled(true);
        invalidProperties.getCashier().setUsername("invalid-cashier-" + suffix);
        invalidProperties.getCashier().setPassword("cashier-test-password");
        invalidProperties.getCashier().setDisplayName("测试收银员");
        invalidProperties.getAdmin().setUsername("invalid-admin-" + suffix);
        invalidProperties.getAdmin().setDisplayName("测试管理员");
        AuthBootstrapRunner runner = new AuthBootstrapRunner(bootstrapService, invalidProperties);

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("cashier-test-password");
        assertThat(accountRepository.findByUsername(new AccountUsername("invalid-cashier-" + suffix)))
                .isEmpty();
        assertThat(accountRepository.findByUsername(new AccountUsername("invalid-admin-" + suffix)))
                .isEmpty();
    }

    @Test
    @DisplayName("TC-S2-DATA-008 初始化不会重新启用或修复已停用账号")
    void doesNotReactivateAnExistingDisabledAccount() {
        String suffix = UUID.randomUUID().toString();
        BootstrapAccount cashier =
                BootstrapAccount.of("disabled-cashier-" + suffix, "disabled-test-password", "待停用收银员", RoleCode.CASHIER);
        BootstrapAccount admin =
                BootstrapAccount.of("disabled-admin-" + suffix, "admin-test-password", "测试管理员", RoleCode.ADMIN);
        bootstrapService.initialize(cashier, admin);
        Account original = accountRepository.findByUsername(cashier.username()).orElseThrow();
        jdbcClient
                .sql("UPDATE accounts SET status = 'DISABLED' WHERE id = CAST(:accountId AS UUID)")
                .param("accountId", original.id().toString())
                .update();

        bootstrapService.initialize(cashier, admin);

        Account disabled = accountRepository.findByUsername(cashier.username()).orElseThrow();
        assertThat(disabled.status()).isEqualTo(AccountStatus.DISABLED);
        assertThat(disabled.id()).isEqualTo(original.id());
        assertThat(disabled.passwordHash()).isEqualTo(original.passwordHash());
        assertThat(disabled.displayName()).isEqualTo(original.displayName());
    }
}
