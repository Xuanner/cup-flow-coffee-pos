package com.cupflow.pos.auth.infrastructure.configuration;

import com.cupflow.pos.auth.infrastructure.security.AuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class AuthorizationWebConfiguration implements WebMvcConfigurer {

    private final AuthorizationInterceptor authorizationInterceptor;

    AuthorizationWebConfiguration(AuthorizationInterceptor authorizationInterceptor) {
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/api/v1/**");
    }
}
