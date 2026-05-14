package com.neullabs.regulus.policy.config;

import com.neullabs.regulus.policy.guard.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.List;

/**
 * Auto-configuration for policy enforcement components.
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(PolicyProperties.class)
@ConditionalOnProperty(name = "regulus.ai.policies.enabled", havingValue = "true", matchIfMissing = true)
public class PolicyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PolicyAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public LeiPolicyGuard leiPolicyGuard() {
        log.info("Registering LEI Policy Guard");
        return new LeiPolicyGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public PurposeCodePolicyGuard purposeCodePolicyGuard() {
        log.info("Registering Purpose Code Policy Guard");
        return new PurposeCodePolicyGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsentPolicyGuard consentPolicyGuard() {
        log.info("Registering Consent Policy Guard");
        return new ConsentPolicyGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public PolicyEnforcer policyEnforcer(List<PolicyGuard> guards) {
        log.info("Creating PolicyEnforcer with {} guards", guards.size());
        return new PolicyEnforcer(guards);
    }

    @Bean
    @ConditionalOnMissingBean
    public PolicyGuardAspect policyGuardAspect(PolicyEnforcer policyEnforcer) {
        log.info("Creating PolicyGuard AOP Aspect");
        return new PolicyGuardAspect(policyEnforcer);
    }
}
