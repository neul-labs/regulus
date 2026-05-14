package com.neullabs.regulus.safety.autoconfigure;

import com.neullabs.regulus.killswitch.interceptor.KillSwitchManager;
import com.neullabs.regulus.killswitch.interceptor.InMemoryKillSwitchStateProvider;
import com.neullabs.regulus.killswitch.interceptor.KillSwitchStateProvider;
import com.neullabs.regulus.killswitch.dualcontrol.DualControlKillSwitch;
import com.neullabs.regulus.privacy.filter.JsonPathRedactionFilter;
import com.neullabs.regulus.privacy.filter.PiiPatternFilter;
import com.neullabs.regulus.privacy.filter.PrivacyFilter;
import com.neullabs.regulus.privacy.filter.PrivacyFilterChain;
import com.neullabs.regulus.policy.residency.DataResidencyEnforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for safety features.
 * Wires kill switch, privacy filters, and prompt injection detection.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "regulus.ai.safety", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SafetyProperties.class)
public class SafetyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SafetyAutoConfiguration.class);

    // === Kill Switch ===

    @Bean
    @ConditionalOnMissingBean
    public KillSwitchStateProvider killSwitchStateProvider() {
        log.info("Creating in-memory kill switch state provider");
        return new InMemoryKillSwitchStateProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public KillSwitchManager killSwitchManager(
            ApplicationEventPublisher eventPublisher,
            KillSwitchStateProvider stateProvider) {
        log.info("Creating KillSwitchManager");
        return new KillSwitchManager(eventPublisher, stateProvider);
    }

    // === Privacy Filters ===

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regulus.ai.safety.privacy.pii-pattern", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PiiPatternFilter piiPatternFilter(SafetyProperties properties) {
        log.info("Creating PII pattern filter");
        return new PiiPatternFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regulus.ai.safety.privacy.json-path", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JsonPathRedactionFilter jsonPathRedactionFilter(SafetyProperties properties) {
        var jsonConfig = properties.getPrivacy().getJsonPath();
        // Convert path strings to RedactionRules
        var rules = jsonConfig.getPaths().stream()
            .map(path -> JsonPathRedactionFilter.RedactionRule.of(path, "sensitive"))
            .toList();
        log.info("Creating JSON path redaction filter with {} rules", rules.size());
        return new JsonPathRedactionFilter(rules);
    }

    @Bean
    @ConditionalOnMissingBean
    public PrivacyFilterChain privacyFilterChain(List<PrivacyFilter> filters) {
        log.info("Creating PrivacyFilterChain with {} filters: {}",
            filters.size(),
            filters.stream().map(f -> f.getClass().getSimpleName()).toList());
        return new PrivacyFilterChain(filters);
    }

    // === Prompt Injection Detection ===

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regulus.ai.safety.prompt-injection", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PromptInjectionDetector promptInjectionDetector(SafetyProperties properties) {
        log.info("Creating prompt injection detector");
        return new RuleBasedPromptInjectionDetector();
    }

    // === Safety Guard (combines all safety checks) ===

    @Bean
    @ConditionalOnMissingBean
    public SafetyGuard safetyGuard(
            KillSwitchManager killSwitchManager,
            PrivacyFilterChain privacyFilterChain,
            PromptInjectionDetector promptInjectionDetector) {
        log.info("Creating SafetyGuard");
        return new SafetyGuard(killSwitchManager, privacyFilterChain, promptInjectionDetector);
    }

    // === Dual Control Kill Switch (4-eyes principle) ===

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regulus.ai.safety.kill-switch.dual-control", name = "enabled", havingValue = "true", matchIfMissing = false)
    public DualControlKillSwitch dualControlKillSwitch(
            KillSwitchManager killSwitchManager,
            SafetyProperties properties) {
        var dualControlProps = properties.getKillSwitch().getDualControl();

        DualControlKillSwitch.DualControlConfig config = new DualControlKillSwitch.DualControlConfig();
        config.setDualControlEnabled(true);
        config.setRequiredApprovers(dualControlProps.getRequiredApprovers());
        config.setAllowEmergencyBypass(dualControlProps.isAllowEmergencyBypass());
        config.setAllowSelfApproval(dualControlProps.isAllowSelfApproval());
        config.setAuthorizedApprovers(dualControlProps.getAuthorizedApprovers());

        log.info("Creating DualControlKillSwitch with {} required approvers", config.getRequiredApprovers());
        return new DualControlKillSwitch(killSwitchManager, config);
    }

    // === Data Residency Enforcement ===

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regulus.ai.safety.data-residency", name = "enabled", havingValue = "true", matchIfMissing = false)
    public DataResidencyEnforcer dataResidencyEnforcer(SafetyProperties properties) {
        var residencyProps = properties.getDataResidency();

        DataResidencyEnforcer.DataResidencyConfig config = new DataResidencyEnforcer.DataResidencyConfig();
        config.setAllowedRegions(residencyProps.getAllowedRegions());
        config.setBlockViolations(residencyProps.isBlockViolations());
        config.setEnforceUkResidency(residencyProps.isEnforceUkResidency());
        config.setAllowUnknownRegions(residencyProps.isAllowUnknownRegions());

        log.info("Creating DataResidencyEnforcer with allowed regions: {}", config.getAllowedRegions());
        return new DataResidencyEnforcer(config);
    }
}
