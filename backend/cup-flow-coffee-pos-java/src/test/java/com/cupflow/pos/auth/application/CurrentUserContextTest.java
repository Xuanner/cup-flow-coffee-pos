package com.cupflow.pos.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.cupflow.pos.auth.domain.RoleCode;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CurrentUserContextTest {

    @Test
    @DisplayName("TASK-S2-AUTHZ-03-01 请求上下文在线程间隔离且可清理")
    void isolatesUsersBetweenThreadsAndClearsAfterRequest() {
        CurrentUserContext context = new CurrentUserContext();
        CurrentUser cashier = user("收银员", RoleCode.CASHIER);
        CurrentUser admin = user("管理员", RoleCode.ADMIN);

        CompletableFuture<CurrentUser> cashierResult = CompletableFuture.supplyAsync(() -> boundUser(context, cashier));
        CompletableFuture<CurrentUser> adminResult = CompletableFuture.supplyAsync(() -> boundUser(context, admin));

        assertThat(cashierResult.join()).isEqualTo(cashier);
        assertThat(adminResult.join()).isEqualTo(admin);
        assertThat(context.get()).isEmpty();
    }

    private CurrentUser boundUser(CurrentUserContext context, CurrentUser user) {
        context.set(user);
        try {
            return context.requireCurrentUser();
        } finally {
            context.clear();
        }
    }

    private CurrentUser user(String displayName, RoleCode role) {
        return new CurrentUser(UUID.randomUUID(), displayName, Set.of(role), "/pos");
    }
}
