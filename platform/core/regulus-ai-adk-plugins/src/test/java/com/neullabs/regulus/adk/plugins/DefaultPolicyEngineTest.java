package com.neullabs.regulus.adk.plugins;

import com.neullabs.regulus.compliance.ComplianceProfile;
import com.neullabs.regulus.compliance.ComplianceProfiles;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the pure-Java decision engine without instantiating the
 * ADK-bound {@code RegulusPolicyPlugin}. This module's plugin classes
 * extend ADK's {@code BasePlugin} which requires the ADK runtime on the
 * classpath — present in CI, not necessarily locally.
 */
class DefaultPolicyEngineTest {

    @Test
    void missingPurposeBlockedUnderGdpr() {
        ComplianceProfile gdpr = ComplianceProfiles.byId("gdpr");
        PolicyContext ctx = new PolicyContext(
                null, "subj-1", "user:1", "model", "gemini-2.5-flash", Map.of());

        PolicyDecision decision = DefaultPolicyEngine.evaluate(gdpr, ctx);

        assertThat(decision).isInstanceOf(PolicyDecision.Block.class);
        PolicyDecision.Block block = (PolicyDecision.Block) decision;
        assertThat(block.code()).isEqualTo("missing_purpose");
        assertThat(block.clauseCitation()).contains("5(1)(b)");
    }

    @Test
    void validPurposeAllowsUnderGdpr() {
        ComplianceProfile gdpr = ComplianceProfiles.byId("gdpr");
        PolicyContext ctx = new PolicyContext(
                "claims-triage", "subj-1", "user:1", "model", "gemini-2.5-flash", Map.of());

        PolicyDecision decision = DefaultPolicyEngine.evaluate(gdpr, ctx);

        assertThat(decision).isInstanceOf(PolicyDecision.Allow.class);
    }

    @Test
    void automatedLegalEffectRequiresConfirmationUnderGdpr() {
        ComplianceProfile gdpr = ComplianceProfiles.byId("gdpr");
        PolicyContext ctx = new PolicyContext(
                "credit-decision",
                "subj-1",
                "user:1",
                "model",
                "gemini-2.5-pro",
                Map.of("automated_legal_effect", "true"));

        PolicyDecision decision = DefaultPolicyEngine.evaluate(gdpr, ctx);

        assertThat(decision).isInstanceOf(PolicyDecision.RequireConfirmation.class);
        PolicyDecision.RequireConfirmation rc = (PolicyDecision.RequireConfirmation) decision;
        assertThat(rc.code()).isEqualTo("art_22_safeguard");
    }

    @Test
    void vulnerableCustomerRequiresConfirmationUnderFcaSysc() {
        ComplianceProfile fca = ComplianceProfiles.byId("fca-sysc");
        PolicyContext ctx = new PolicyContext(
                "retail-mortgage-advice",
                "subj-1",
                "user:1",
                "model",
                "gemini-2.5-pro",
                Map.of("vulnerable_customer", "true"));

        PolicyDecision decision = DefaultPolicyEngine.evaluate(fca, ctx);

        assertThat(decision).isInstanceOf(PolicyDecision.RequireConfirmation.class);
        PolicyDecision.RequireConfirmation rc = (PolicyDecision.RequireConfirmation) decision;
        assertThat(rc.code()).isEqualTo("vulnerable_customer");
    }
}
