package com.neullabs.regulus.compliance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComplianceProfilesTest {

    @Test
    void allCatalogueIdsResolve() {
        for (String id : ComplianceProfiles.all().keySet()) {
            assertThat(ComplianceProfiles.byId(id))
                    .as("profile id: %s", id)
                    .isNotNull()
                    .extracting(ComplianceProfile::id)
                    .isEqualTo(id);
        }
    }

    @Test
    void unknownIdThrows() {
        assertThatThrownBy(() -> ComplianceProfiles.byId("does-not-exist"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does-not-exist")
                .hasMessageContaining("Known:");
    }

    @Test
    void composeProducesCompositeWithEveryMemberAccessible() {
        ComplianceProfile composite = ComplianceProfiles.compose(
                List.of("eu-ai-act", "uk-gdpr", "fca-sysc"));

        assertThat(composite).isInstanceOf(CompositeComplianceProfile.class);
        assertThat(((CompositeComplianceProfile) composite).components())
                .hasSize(3)
                .extracting(ComplianceProfile::id)
                .containsExactly("eu-ai-act", "uk-gdpr", "fca-sysc");
    }

    @Test
    void catalogueContainsExpectedProfiles() {
        assertThat(ComplianceProfiles.all().keySet()).containsExactlyInAnyOrder(
                "eu-ai-act", "gdpr", "uk-gdpr", "dora", "nis2",
                "fca-sysc", "pra-ss1-23", "pra-ss2-21", "nhs-dspt", "ehds");
    }
}
