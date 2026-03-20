package com.regulus.platform.governance.autoconfigure;

import com.regulus.platform.observability.audit.AuditLogger;
import com.regulus.platform.policy.guard.ConsentPolicyGuard;
import com.regulus.platform.policy.guard.LeiPolicyGuard;
import com.regulus.platform.policy.guard.PolicyEnforcer;
import com.regulus.platform.policy.guard.PolicyGuard;
import com.regulus.platform.policy.guard.PurposeCodePolicyGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for governance features.
 * Wires policy guards and enforcement mechanisms.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "regulus.ai.governance", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GovernanceProperties.class)
public class GovernanceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GovernanceAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regulus.ai.governance.guards.lei", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LeiPolicyGuard leiPolicyGuard(GovernanceProperties properties) {
        log.info("Registering LEI policy guard");
        return new LeiPolicyGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regulus.ai.governance.guards.purpose-code", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PurposeCodePolicyGuard purposeCodePolicyGuard(GovernanceProperties properties) {
        log.info("Registering Purpose Code policy guard");
        return new PurposeCodePolicyGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regulus.ai.governance.guards.consent", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ConsentPolicyGuard consentPolicyGuard() {
        log.info("Registering Consent policy guard");
        return new ConsentPolicyGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public PolicyEnforcer policyEnforcer(List<PolicyGuard> guards) {
        log.info("Creating PolicyEnforcer with {} guards: {}",
            guards.size(),
            guards.stream().map(g -> g.getClass().getSimpleName()).toList());
        return new PolicyEnforcer(guards);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogger auditLogger() {
        return new AuditLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    public GovernanceInterceptor governanceInterceptor(
            PolicyEnforcer policyEnforcer,
            AuditLogger auditLogger,
            GovernanceProperties properties) {
        log.info("Creating GovernanceInterceptor: enforceMode={}",
            properties.isEnforceMode() ? "BLOCK" : "AUDIT_ONLY");
        return new GovernanceInterceptor(policyEnforcer, auditLogger, properties.isEnforceMode());
    }
}
