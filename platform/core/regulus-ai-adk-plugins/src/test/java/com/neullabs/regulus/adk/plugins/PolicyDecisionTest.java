package com.neullabs.regulus.adk.plugins;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDecisionTest {

    @Test
    void allowSingletonInstancePresent() {
        assertThat(PolicyDecision.ALLOW).isInstanceOf(PolicyDecision.Allow.class);
    }

    @Test
    void blockCarriesCodeReasonAndCitation() {
        PolicyDecision.Block block = new PolicyDecision.Block(
                "missing_purpose",
                "purpose_code is required",
                "GDPR Art. 5(1)(b)");
        assertThat(block.code()).isEqualTo("missing_purpose");
        assertThat(block.reason()).contains("required");
        assertThat(block.clauseCitation()).isEqualTo("GDPR Art. 5(1)(b)");
    }

    @Test
    void requireConfirmationCarriesCodeAndReason() {
        PolicyDecision.RequireConfirmation rc = new PolicyDecision.RequireConfirmation(
                "vulnerable_customer", "enhanced HITL required");
        assertThat(rc.code()).isEqualTo("vulnerable_customer");
        assertThat(rc.reason()).contains("HITL");
    }
}
