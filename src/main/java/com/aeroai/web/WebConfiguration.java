package com.aeroai.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final SessionGuard sessionGuard;
    public WebConfiguration(SessionGuard sessionGuard) { this.sessionGuard = sessionGuard; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionGuard).addPathPatterns("/dashboard", "/lab/**", "/api/readings/**", "/api/experiments/**",
                "/api/reliability/**", "/api/air-quality/**", "/api/alerts/**", "/support/**", "/api/assessment/**", "/api/assistant");
    }
}
