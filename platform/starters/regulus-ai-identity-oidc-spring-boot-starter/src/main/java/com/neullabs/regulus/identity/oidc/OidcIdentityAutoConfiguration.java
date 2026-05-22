package com.neullabs.regulus.identity.oidc;

import com.neullabs.regulus.identity.spi.IdentityAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Registers the reference {@link OidcIdentityAdapter} and the
 * {@link OidcSecurityContextFilter} when Spring Security's OAuth2 resource
 * server classes are on the runtime classpath.
 *
 * <p>Both beans are {@code @ConditionalOnMissingBean}, so tenants who want
 * a bespoke claim mapping just register their own {@link IdentityAdapter}
 * bean and this autoconfig steps aside.
 */
@AutoConfiguration
@ConditionalOnClass(JwtAuthenticationToken.class)
@ConditionalOnProperty(name = "regulus.identity.oidc.enabled", havingValue = "true", matchIfMissing = true)
public class OidcIdentityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OidcIdentityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(IdentityAdapter.class)
    public IdentityAdapter oidcIdentityAdapter() {
        log.info("Registering Regulus OidcIdentityAdapter (JwtAuthenticationToken → Identity)");
        return new OidcIdentityAdapter();
    }

    @Bean
    @ConditionalOnMissingBean
    public OidcSecurityContextFilter oidcSecurityContextFilter(IdentityAdapter adapter) {
        return new OidcSecurityContextFilter(adapter);
    }
}
