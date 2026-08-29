package com.cupflow.pos.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.cupflow.pos.auth.domain.RoleCode;
import com.cupflow.pos.shared.security.EndpointRole;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoleAuthorizationTest {

    private final RoleAuthorization authorization = new RoleAuthorization();

    @Test
    @DisplayName("TASK-S2-AUTHZ-03-01 ADMIN 继承 CASHIER 权限")
    void adminIncludesCashierPermissions() {
        CurrentUser admin = user(Set.of(RoleCode.ADMIN));

        assertThat(authorization.allows(admin, EndpointRole.CASHIER)).isTrue();
        assertThat(authorization.allows(admin, EndpointRole.ADMIN)).isTrue();
    }

    @Test
    @DisplayName("TASK-S2-AUTHZ-03-01 CASHIER 不具备 ADMIN 权限")
    void cashierDoesNotIncludeAdminPermissions() {
        CurrentUser cashier = user(Set.of(RoleCode.CASHIER));

        assertThat(authorization.allows(cashier, EndpointRole.CASHIER)).isTrue();
        assertThat(authorization.allows(cashier, EndpointRole.ADMIN)).isFalse();
    }

    @Test
    @DisplayName("TASK-S2-AUTHZ-03-01 多角色按权限并集计算")
    void multipleRolesUseTheirUnion() {
        CurrentUser multiRole = user(Set.of(RoleCode.CASHIER, RoleCode.ADMIN));

        assertThat(authorization.allows(multiRole, EndpointRole.CASHIER)).isTrue();
        assertThat(authorization.allows(multiRole, EndpointRole.ADMIN)).isTrue();
    }

    private CurrentUser user(Set<RoleCode> roles) {
        return new CurrentUser(UUID.randomUUID(), "权限测试用户", roles, "/pos");
    }
}
