package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.RoleCode;
import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID id, String displayName, Set<RoleCode> roles, String defaultPath) {

    public static CurrentUser from(Account account) {
        String defaultPath = account.roles().contains(RoleCode.ADMIN) ? "/dashboard" : "/pos";
        return new CurrentUser(account.id(), account.displayName(), account.roles(), defaultPath);
    }
}
