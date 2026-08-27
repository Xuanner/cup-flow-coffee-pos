package com.cupflow.pos.auth.infrastructure.configuration;

import com.cupflow.pos.auth.application.AccountBootstrapService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthBootstrapProperties.class)
class AuthBootstrapConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "auth.bootstrap", name = "enabled", havingValue = "true")
    AuthBootstrapRunner authBootstrapRunner(
            AccountBootstrapService bootstrapService, AuthBootstrapProperties properties) {
        return new AuthBootstrapRunner(bootstrapService, properties);
    }
}
