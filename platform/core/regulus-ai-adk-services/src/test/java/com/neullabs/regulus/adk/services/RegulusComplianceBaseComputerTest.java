package com.neullabs.regulus.adk.services;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegulusComplianceBaseComputerTest {

    @Test
    void carriesAllowedDomainsAndHighRiskActions() {
        Set<RegulusComplianceBaseComputer.HighRiskAction> highRisk = Set.of(
                RegulusComplianceBaseComputer.HighRiskAction.FORM_SUBMIT,
                RegulusComplianceBaseComputer.HighRiskAction.PAYMENT_CONFIRM);

        RegulusComplianceBaseComputer c = new RegulusComplianceBaseComputer(
                Set.of("portal.example.com"), true, highRisk);

        assertThat(c.allowedDomains()).containsExactly("portal.example.com");
        assertThat(c.redactScreenshots()).isTrue();
        assertThat(c.requiresConfirmation()).hasSize(2);
    }

    @Test
    void allHighRiskActionsCanBeRequested() {
        // Sanity: every enum value should be constructible into the requiresConfirmation set.
        Set<RegulusComplianceBaseComputer.HighRiskAction> all =
                Set.of(RegulusComplianceBaseComputer.HighRiskAction.values());
        new RegulusComplianceBaseComputer(Set.of(), false, all);
        assertThat(all).hasSizeGreaterThanOrEqualTo(5);
    }
}
