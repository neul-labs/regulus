package com.neullabs.regulus.adk.services;

import com.neullabs.regulus.compliance.ComplianceProfile;
import com.neullabs.regulus.compliance.ComplianceProfiles;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RegulusRetentionEventCompactorTest {

    @Test
    void dropsBeyondSummaryRetention() {
        ComplianceProfile gdpr = ComplianceProfiles.byId("gdpr");
        RegulusRetentionEventCompactor compactor = new RegulusRetentionEventCompactor(gdpr);
        assertThat(compactor.shouldDrop(compactor.summaryRetention().plusDays(1))).isTrue();
        assertThat(compactor.shouldDrop(compactor.summaryRetention().minusDays(1))).isFalse();
    }

    @Test
    void summarisesBetweenFullAndSummary() {
        ComplianceProfile gdpr = ComplianceProfiles.byId("gdpr");
        RegulusRetentionEventCompactor compactor = new RegulusRetentionEventCompactor(gdpr);
        Duration midRange = compactor.fullRetention().plusDays(1);
        assertThat(compactor.shouldSummarise(midRange)).isTrue();
    }

    @Test
    void neitherDropsNorSummarisesWithinFullRetention() {
        ComplianceProfile gdpr = ComplianceProfiles.byId("gdpr");
        RegulusRetentionEventCompactor compactor = new RegulusRetentionEventCompactor(gdpr);
        Duration fresh = compactor.fullRetention().minusDays(1);
        assertThat(compactor.shouldDrop(fresh)).isFalse();
        assertThat(compactor.shouldSummarise(fresh)).isFalse();
    }

    @Test
    void compositeUsesLongestRetention() {
        // gdpr = 1y full; dora = 5y full. Composite picks 5y.
        ComplianceProfile composite = ComplianceProfiles.compose(java.util.List.of("gdpr", "dora"));
        RegulusRetentionEventCompactor compactor = new RegulusRetentionEventCompactor(composite);
        assertThat(compactor.fullRetention()).isEqualTo(Duration.ofDays(365 * 5));
    }
}
