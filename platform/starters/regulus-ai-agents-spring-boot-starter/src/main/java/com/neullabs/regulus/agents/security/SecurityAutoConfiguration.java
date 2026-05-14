package com.neullabs.regulus.agents.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashSet;
import java.util.Set;

/**
 * Auto-configuration for security hardening.
 * Configures API key authentication, rate limiting, and mTLS.
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(name = "regulus.ai.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    // Paths that should be excluded from authentication
    private static final Set<String> DEFAULT_EXCLUDED_PATHS = Set.of(
        "/actuator/health",
        "/actuator/info",
        "/.well-known/*",
        "/error"
    );

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.security.oauth2.enabled", havingValue = "true")
    public JwtTokenValidator jwtTokenValidator(SecurityProperties properties) {
        log.info("Creating JWT token validator for provider: {}", properties.getOauth2().getProvider());
        return new JwtTokenValidator(properties.getOauth2());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.security.oauth2.enabled", havingValue = "true")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<OAuth2JwtFilter> oauth2JwtFilterRegistration(
            SecurityProperties properties, JwtTokenValidator tokenValidator) {

        Set<String> excludedPaths = new HashSet<>(DEFAULT_EXCLUDED_PATHS);

        OAuth2JwtFilter filter = new OAuth2JwtFilter(tokenValidator, excludedPaths);

        FilterRegistrationBean<OAuth2JwtFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5); // Before API key filter
        registration.setName("oauth2JwtFilter");

        log.info("Registered OAuth2 JWT filter for provider: {}", properties.getOauth2().getProvider());
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.security.api-key.enabled", havingValue = "true")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration(SecurityProperties properties) {
        SecurityProperties.ApiKeyConfig config = properties.getApiKey();

        Set<String> validKeys = new HashSet<>(config.getValidKeys());
        Set<String> excludedPaths = new HashSet<>(DEFAULT_EXCLUDED_PATHS);

        ApiKeyFilter filter = new ApiKeyFilter(config.getHeaderName(), validKeys, excludedPaths);

        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("apiKeyFilter");

        log.info("Registered API key filter");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.security.rate-limit.enabled", havingValue = "true")
    public RateLimiter rateLimiter(SecurityProperties properties) {
        SecurityProperties.RateLimitConfig config = properties.getRateLimit();
        return new RateLimiter(config.getRequestsPerSecond(), config.getBurstCapacity());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.security.rate-limit.enabled", havingValue = "true")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            SecurityProperties properties, RateLimiter rateLimiter) {

        SecurityProperties.RateLimitConfig config = properties.getRateLimit();
        String apiKeyHeader = properties.getApiKey().getHeaderName();

        RateLimitFilter filter = new RateLimitFilter(
            rateLimiter,
            config.isByClientIp(),
            config.isByClient(),
            apiKeyHeader
        );

        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20); // After API key filter
        registration.setName("rateLimitFilter");

        log.info("Registered rate limit filter");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.security.mtls.enabled", havingValue = "true")
    public MtlsWebClientCustomizer mtlsWebClientCustomizer(SecurityProperties properties) {
        return new MtlsWebClientCustomizer(properties.getMtls());
    }

    @Bean
    @ConditionalOnProperty(name = "regulus.ai.security.mtls.enabled", havingValue = "true")
    public WebClient.Builder mtlsWebClientBuilder(MtlsWebClientCustomizer customizer) {
        log.info("Creating mTLS-enabled WebClient builder");
        return customizer.createBuilder();
    }
}
