package com.cupflow.pos.auth.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.bootstrap")
public class AuthBootstrapProperties {

    private boolean enabled;
    private Credential cashier = new Credential();
    private Credential admin = new Credential();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Credential getCashier() {
        return cashier;
    }

    public void setCashier(Credential cashier) {
        this.cashier = cashier;
    }

    public Credential getAdmin() {
        return admin;
    }

    public void setAdmin(Credential admin) {
        this.admin = admin;
    }

    public static class Credential {

        private String username;
        private String password;
        private String displayName;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
