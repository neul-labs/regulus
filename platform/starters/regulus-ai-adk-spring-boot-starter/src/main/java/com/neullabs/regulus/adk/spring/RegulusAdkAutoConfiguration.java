package com.neullabs.regulus.adk.spring;

import com.neullabs.regulus.adk.plugins.AuditSink;
import com.neullabs.regulus.adk.plugins.RegulusAuditPlugin;
import com.neullabs.regulus.adk.plugins.RegulusDataResidencyPlugin;
import com.neullabs.regulus.adk.plugins.RegulusGovernanceEvidencePlugin;
import com.neullabs.regulus.adk.plugins.RegulusKillSwitchPlugin;
import com.neullabs.regulus.adk.plugins.RegulusModelRiskPlugin;
import com.neullabs.regulus.adk.plugins.RegulusPolicyPlugin;
import com.neullabs.regulus.adk.plugins.RegulusPrivacyPlugin;
import com.neullabs.regulus.compliance.ComplianceProfile;
import com.neullabs.regulus.compliance.ComplianceProfiles;
import com.neullabs.regulus.compliance.ResidencyPolicy;
import com.neullabs.regulus.governance.GovernanceFramework;
import com.neullabs.regulus.governance.GovernanceFrameworks;
import com.neullabs.regulus.grc.AdapterHealthCheck;
import com.neullabs.regulus.grc.GrcEvidenceAdapter;
import com.neullabs.regulus.grc.adapter.MetricStreamAdapter;
import com.neullabs.regulus.grc.adapter.OneTrustAiGovernanceAdapter;
import com.neullabs.regulus.grc.adapter.ServiceNowIrmAdapter;
import com.neullabs.regulus.grc.adapter.StdoutAdapter;
import com.neullabs.regulus.grc.adapter.WebhookAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
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

    // ──────────────────────────────────────────────────────────────────
    // Governance + GRC wiring (opt-in)
    // ──────────────────────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    public GovernanceFramework regulusGovernanceFramework(RegulusAdkProperties props) {
        if (props.getGovernance().getFrameworks().isEmpty()) {
            return null; // Governance is voluntary; absence is valid.
        }
        return GovernanceFrameworks.compose(props.getGovernance().getFrameworks());
    }

    @Bean
    @ConditionalOnMissingBean
    public List<GrcEvidenceAdapter> regulusGrcAdapters(RegulusAdkProperties props) {
        List<GrcEvidenceAdapter> adapters = new ArrayList<>();
        RegulusAdkProperties.Grc grc = props.getGrc();

        if (grc.isStdout()) {
            adapters.add(new StdoutAdapter());
        }

        if (grc.getServicenowIrm().isEnabled()) {
            RegulusAdkProperties.Grc.ServiceNow s = grc.getServicenowIrm();
            URI base = URI.create(s.getBaseUri());
            if (s.getBearerToken() != null && !s.getBearerToken().isBlank()) {
                adapters.add(new ServiceNowIrmAdapter(base, s.getBearerToken(), null));
            } else {
                adapters.add(new ServiceNowIrmAdapter(base, s.getUsername(), s.getPassword(), null));
            }
        }

        if (grc.getOnetrustAiGov().isEnabled()) {
            RegulusAdkProperties.Grc.OneTrust o = grc.getOnetrustAiGov();
            adapters.add(new OneTrustAiGovernanceAdapter(
                    URI.create(o.getBaseUri()), o.getApiKey(), null));
        }

        if (grc.getMetricstream().isEnabled()) {
            RegulusAdkProperties.Grc.MetricStream m = grc.getMetricstream();
            adapters.add(new MetricStreamAdapter(
                    URI.create(m.getBaseUri()), m.getAuthToken(), m.getIntakeAppName(), null));
        }

        if (grc.getWebhook().isEnabled()) {
            RegulusAdkProperties.Grc.Webhook w = grc.getWebhook();
            byte[] hmacKey = HexFormat.of().parseHex(w.getHmacKeyHex());
            adapters.add(new WebhookAdapter(URI.create(w.getEndpoint()), hmacKey));
        }

        if (!adapters.isEmpty()) {
            AdapterHealthCheck.verify(adapters);
        }
        return adapters;
    }

    @Bean
    @ConditionalOnMissingBean
    public RegulusGovernanceEvidencePlugin regulusGovernanceEvidencePlugin(
            GovernanceFramework framework,
            List<GrcEvidenceAdapter> adapters,
            AuditSink auditSink) {
        if (framework == null || adapters.isEmpty()) {
            return null; // No governance configured + no adapters => no plugin.
        }
        return RegulusGovernanceEvidencePlugin.forFramework(framework, adapters, auditSink);
    }
}
