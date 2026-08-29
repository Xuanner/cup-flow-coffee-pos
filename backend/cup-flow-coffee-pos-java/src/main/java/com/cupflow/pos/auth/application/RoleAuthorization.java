package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.RoleCode;
import com.cupflow.pos.shared.security.EndpointRole;
import org.springframework.stereotype.Component;

@Component
public class RoleAuthorization {

    public boolean allows(CurrentUser user, EndpointRole requiredRole) {
        RoleCode required = RoleCode.valueOf(requiredRole.name());
        return user.roles().contains(required)
                || (required == RoleCode.CASHIER && user.roles().contains(RoleCode.ADMIN));
    }
}
