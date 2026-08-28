package com.cupflow.pos.auth.infrastructure.configuration;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthSecurityProperties.class)
@EnableScheduling
class AuthSecurityConfiguration {

    @Bean
    Clock authenticationClock() {
        return Clock.systemUTC();
    }
}
