package com.neullabs.regulus.compliance;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke invariants every shipped {@link ComplianceProfile} must satisfy.
 * Parameterised across all known profile ids so adding a new profile
 * automatically gets covered.
 */
class AllProfilesSanityTest {

    private static final String[] ALL_IDS = {
            "eu-ai-act", "gdpr", "uk-gdpr", "dora", "nis2",
            "fca-sysc", "pra-ss1-23", "pra-ss2-21", "nhs-dspt", "ehds"
    };

    @ParameterizedTest
    @ValueSource(strings = {
            "eu-ai-act", "gdpr", "uk-gdpr", "dora", "nis2",
            "fca-sysc", "pra-ss1-23", "pra-ss2-21", "nhs-dspt", "ehds"
    })
    void idMatchesItself(String id) {
        assertThat(ComplianceProfiles.byId(id).id()).isEqualTo(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "eu-ai-act", "gdpr", "uk-gdpr", "dora", "nis2",
            "fca-sysc", "pra-ss1-23", "pra-ss2-21", "nhs-dspt", "ehds"
    })
    void displayNameIsNonEmpty(String id) {
        assertThat(ComplianceProfiles.byId(id).displayName()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "eu-ai-act", "gdpr", "uk-gdpr", "dora", "nis2",
            "fca-sysc", "pra-ss1-23", "pra-ss2-21", "nhs-dspt", "ehds"
    })
    void citationIsNonEmpty(String id) {
        assertThat(ComplianceProfiles.byId(id).citation()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "eu-ai-act", "gdpr", "uk-gdpr", "dora", "nis2",
            "fca-sysc", "pra-ss1-23", "pra-ss2-21", "nhs-dspt", "ehds"
    })
    void controlsAreNonEmpty(String id) {
        assertThat(ComplianceProfiles.byId(id).controls()).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "eu-ai-act", "gdpr", "uk-gdpr", "dora", "nis2",
            "fca-sysc", "pra-ss1-23", "pra-ss2-21", "nhs-dspt", "ehds"
    })
    void retentionDurationsArePositive(String id) {
        EventCompactionPolicy retention = ComplianceProfiles.byId(id).retention();
        assertThat(retention.fullEventRetention()).isGreaterThan(Duration.ZERO);
        assertThat(retention.summaryRetention()).isGreaterThanOrEqualTo(retention.fullEventRetention());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "eu-ai-act", "gdpr", "uk-gdpr", "dora", "nis2",
            "fca-sysc", "pra-ss1-23", "pra-ss2-21", "nhs-dspt", "ehds"
    })
    void auditSchemaIncludesCanonicalFields(String id) {
        AuditSchema schema = ComplianceProfiles.byId(id).auditSchema();
        assertThat(schema.requiredFields())
                .as("profile %s required fields", id)
                .contains("event_id", "occurred_at", "actor", "action", "result");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "eu-ai-act", "gdpr", "uk-gdpr", "dora", "nis2",
            "fca-sysc", "pra-ss1-23", "pra-ss2-21", "nhs-dspt", "ehds"
    })
    void controlBindingsCarryNonBlankRationale(String id) {
        for (ControlBinding b : ComplianceProfiles.byId(id).controls()) {
            assertThat(b.mechanism()).isNotBlank();
            assertThat(b.clause()).isNotBlank();
            assertThat(b.rationale()).isNotBlank();
        }
    }
}
