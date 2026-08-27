package com.cupflow.pos.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.PasswordHasher;
import com.cupflow.pos.auth.domain.RoleCode;
import org.junit.jupiter.api.Test;

class AccountBootstrapServiceTest {

    @Test
    void rejectsDuplicateNormalizedUsernamesBeforeHashingOrWriting() {
        AccountRepository repository = mock(AccountRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);
        AccountBootstrapService service = new AccountBootstrapService(repository, passwordHasher);
        BootstrapAccount cashier =
                BootstrapAccount.of(" shared-account ", "cashier-test-password", "测试收银员", RoleCode.CASHIER);
        BootstrapAccount admin = BootstrapAccount.of("shared-account", "admin-test-password", "测试管理员", RoleCode.ADMIN);

        assertThatThrownBy(() -> service.initialize(cashier, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different");
        verifyNoInteractions(repository, passwordHasher);
    }
}
