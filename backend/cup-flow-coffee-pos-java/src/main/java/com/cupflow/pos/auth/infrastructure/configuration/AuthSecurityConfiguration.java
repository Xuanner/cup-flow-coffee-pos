package com.cupflow.pos.auth.infrastructure.configuration;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthSecurityProperties.class)
class AuthSecurityConfiguration {

    @Bean
    Clock authenticationClock() {
        return Clock.systemUTC();
    }
}
