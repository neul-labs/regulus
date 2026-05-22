package com.neullabs.regulus.compliance;

import com.neullabs.regulus.identity.Jurisdiction;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeComplianceProfileTest {

    @Test
    void emptyCompositeRejected() {
        assertThatThrownBy(() -> new CompositeComplianceProfile(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compositeIdJoinsComponents() {
        ComplianceProfile composite = ComplianceProfiles.compose(
                List.of("eu-ai-act", "uk-gdpr"));
        assertThat(composite.id()).contains("eu-ai-act").contains("uk-gdpr").contains("+");
    }

    @Test
    void retentionWindowIsLongestAcross() {
        ComplianceProfile composite = ComplianceProfiles.compose(
                List.of("gdpr", "dora")); // dora's 5y > gdpr's 1y
        Duration fullRetention = composite.retention().fullEventRetention();
        assertThat(fullRetention)
                .isGreaterThanOrEqualTo(ComplianceProfiles.byId("dora").retention().fullEventRetention());
    }

    @Test
    void residencyAllowlistIsIntersection() {
        ComplianceProfile composite = ComplianceProfiles.compose(
                List.of("gdpr", "uk-gdpr")); // uk-gdpr restricts to europe-west2 only
        assertThat(composite.residency().allowedRegions())
                .containsExactly("europe-west2");
    }

    @Test
    void cmekTrueIfAnyComponentRequiresIt() {
        ComplianceProfile composite = ComplianceProfiles.compose(
                List.of("gdpr", "fca-sysc")); // fca-sysc requires CMEK; gdpr doesn't
        assertThat(composite.residency().requireCmek()).isTrue();
    }

    @Test
    void auditFieldsAreUnionOfComponents() {
        ComplianceProfile composite = ComplianceProfiles.compose(
                List.of("eu-ai-act", "fca-sysc"));
        assertThat(composite.auditSchema().requiredFields())
                .contains("model_id")              // from eu-ai-act
                .contains("smf_holder");           // from fca-sysc
    }

    @Test
    void immutabilityIsStrongestAcross() {
        // fca-sysc declares SIGNED; gdpr declares MONOTONIC → composite is SIGNED.
        ComplianceProfile composite = ComplianceProfiles.compose(
                List.of("gdpr", "fca-sysc"));
        assertThat(composite.auditSchema().immutabilityHint())
                .isEqualTo(AuditSchema.Immutability.SIGNED);
    }

    @Test
    void jurisdictionFromComponents() {
        assertThat(ComplianceProfiles.compose(List.of("gdpr", "uk-gdpr")).jurisdiction())
                .isEqualTo(Jurisdiction.EU_UK);
        assertThat(ComplianceProfiles.compose(List.of("gdpr", "dora")).jurisdiction())
                .isEqualTo(Jurisdiction.EU);
        assertThat(ComplianceProfiles.compose(List.of("uk-gdpr", "fca-sysc")).jurisdiction())
                .isEqualTo(Jurisdiction.UK);
    }
}
