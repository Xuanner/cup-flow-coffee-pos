package com.cupflow.pos.auth.infrastructure.configuration;

import com.cupflow.pos.auth.application.AccountBootstrapService;
import com.cupflow.pos.auth.application.BootstrapAccount;
import com.cupflow.pos.auth.domain.RoleCode;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class AuthBootstrapRunner implements ApplicationRunner {

    private final AccountBootstrapService bootstrapService;
    private final AuthBootstrapProperties properties;

    public AuthBootstrapRunner(AccountBootstrapService bootstrapService, AuthBootstrapProperties properties) {
        this.bootstrapService = bootstrapService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        AuthBootstrapProperties.Credential cashierProperties = properties.getCashier();
        AuthBootstrapProperties.Credential adminProperties = properties.getAdmin();
        if (cashierProperties == null || adminProperties == null) {
            throw new IllegalStateException("Bootstrap account configuration is incomplete");
        }

        BootstrapAccount cashier = BootstrapAccount.of(
                cashierProperties.getUsername(),
                cashierProperties.getPassword(),
                cashierProperties.getDisplayName(),
                RoleCode.CASHIER);
        BootstrapAccount admin = BootstrapAccount.of(
                adminProperties.getUsername(),
                adminProperties.getPassword(),
                adminProperties.getDisplayName(),
                RoleCode.ADMIN);
        bootstrapService.initialize(cashier, admin);
    }
}
