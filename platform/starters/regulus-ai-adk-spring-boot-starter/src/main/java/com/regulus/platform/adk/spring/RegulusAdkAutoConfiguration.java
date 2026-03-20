package com.regulus.platform.adk.spring;

import com.regulus.platform.adk.plugins.AuditSink;
import com.regulus.platform.adk.plugins.RegulusAuditPlugin;
import com.regulus.platform.adk.plugins.RegulusDataResidencyPlugin;
import com.regulus.platform.adk.plugins.RegulusKillSwitchPlugin;
import com.regulus.platform.adk.plugins.RegulusModelRiskPlugin;
import com.regulus.platform.adk.plugins.RegulusPolicyPlugin;
import com.regulus.platform.adk.plugins.RegulusPrivacyPlugin;
import com.regulus.platform.compliance.ComplianceProfile;
import com.regulus.platform.compliance.ComplianceProfiles;
import com.regulus.platform.compliance.ResidencyPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Wires the Regulus ADK plugin suite into a Spring Boot application based on
 * {@link RegulusAdkProperties}. Each bean is {@code @ConditionalOnMissingBean}
 * so the user can override any single piece without losing the rest.
 *
 * <p>The plugins land on the {@code App.builder()} via the
 * {@code RegulusAppCustomizer} that the consuming code can inject, or via
 * the optional {@code RegulusAdkApp} convenience class.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RegulusAdkProperties.class)
public class RegulusAdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ComplianceProfile regulusComplianceProfile(RegulusAdkProperties props) {
        if (props.getCompliance().getProfiles().isEmpty()) {
            throw new IllegalStateException(
                    "regulus.compliance.profiles must declare at least one profile. " +
                    "Known profiles: " + ComplianceProfiles.all().keySet());
        }
        return ComplianceProfiles.compose(props.getCompliance().getProfiles());
    }

    @Bean
    @ConditionalOnMissingBean
    public RegulusPolicyPlugin regulusPolicyPlugin(ComplianceProfile profile) {
        return RegulusPolicyPlugin.fromProfile(profile);
    }

    @Bean
    @ConditionalOnMissingBean
    public RegulusPrivacyPlugin regulusPrivacyPlugin() {
        return RegulusPrivacyPlugin.withPatterns(
                RegulusPrivacyPlugin.BuiltInPattern.NINO,
                RegulusPrivacyPlugin.BuiltInPattern.IBAN,
                RegulusPrivacyPlugin.BuiltInPattern.BIC,
                RegulusPrivacyPlugin.BuiltInPattern.SORT_CODE,
                RegulusPrivacyPlugin.BuiltInPattern.UK_ACCOUNT_NUMBER,
                RegulusPrivacyPlugin.BuiltInPattern.UK_POSTCODE,
                RegulusPrivacyPlugin.BuiltInPattern.EMAIL
        ).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditSink regulusAuditSink(RegulusAdkProperties props) {
        return switch (props.getAdk().getAudit().getSink()) {
            case "kafka" -> AuditSink.kafka(props.getAdk().getAudit().getKafkaTopic());
            default      -> AuditSink.stdout();
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public RegulusAuditPlugin regulusAuditPlugin(ComplianceProfile profile, AuditSink sink) {
        return RegulusAuditPlugin.forProfile(profile).toSink(sink).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public RegulusKillSwitchPlugin regulusKillSwitchPlugin(RegulusAdkProperties props) {
        if (!props.getAdk().getKillSwitch().isEnabled()) return null;
        return RegulusKillSwitchPlugin.dualControl();
    }

    @Bean
    @ConditionalOnMissingBean
    public RegulusDataResidencyPlugin regulusDataResidencyPlugin(RegulusAdkProperties props,
                                                                 ComplianceProfile profile) {
        Set<String> allowed = props.getAdk().getResidency().getAllowedRegions().isEmpty()
                ? profile.residency().allowedRegions()
                : Set.copyOf(props.getAdk().getResidency().getAllowedRegions());
        boolean cmek = props.getAdk().getResidency().isRequireCmek() || profile.residency().requireCmek();
        return RegulusDataResidencyPlugin.fromPolicy(new ResidencyPolicy(
                allowed, cmek, profile.residency().crossBorderTransfer()));
    }

    @Bean
    @ConditionalOnMissingBean
    public RegulusModelRiskPlugin regulusModelRiskPlugin(RegulusAdkProperties props) {
        RegulusModelRiskPlugin.Tier tier =
                RegulusModelRiskPlugin.Tier.valueOf(props.getAdk().getModelRisk().getTenantTier());
        return RegulusModelRiskPlugin.tier(tier);
    }
}
