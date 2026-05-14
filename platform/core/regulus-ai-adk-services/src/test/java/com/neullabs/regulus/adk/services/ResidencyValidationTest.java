package com.neullabs.regulus.adk.services;

import com.neullabs.regulus.compliance.ResidencyPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the Regulus service wrappers enforce residency at
 * construction time — fail-closed per ADR-008.
 */
class ResidencyValidationTest {

    private static final ResidencyPolicy UK_ONLY =
            new ResidencyPolicy(Set.of("europe-west2"), false,
                    ResidencyPolicy.CrossBorderTransfer.FORBIDDEN);

    private static final ResidencyPolicy UK_WITH_CMEK =
            new ResidencyPolicy(Set.of("europe-west2"), true,
                    ResidencyPolicy.CrossBorderTransfer.FORBIDDEN);

    private static final com.neullabs.regulus.compliance.EventCompactionPolicy DEFAULT_RETENTION =
            com.neullabs.regulus.compliance.EventCompactionPolicy.unconstrained();

    @Test
    void vertexAiSessionRefusesNonAllowlistRegion() {
        assertThatThrownBy(() -> RegulusVertexAiSessionService.wrap(
                "my-project", "us-central1", null, UK_ONLY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("us-central1")
                .hasMessageContaining("europe-west2");
    }

    @Test
    void vertexAiSessionAcceptsAllowlistRegion() {
        RegulusVertexAiSessionService svc = RegulusVertexAiSessionService.wrap(
                "my-project", "europe-west2", null, UK_ONLY);
        assertThat(svc.location()).isEqualTo("europe-west2");
    }

    @Test
    void vertexAiSessionRequiresCmekWhenProfileMandatesIt() {
        assertThatThrownBy(() -> RegulusVertexAiSessionService.wrap(
                "my-project", "europe-west2", null, UK_WITH_CMEK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CMEK");
    }

    @Test
    void firestoreSessionRefusesUsRegion() {
        assertThatThrownBy(() -> RegulusFirestoreSessionService.wrap(
                "my-project", "nam5", UK_ONLY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void firestoreMemoryRefusesNonErasureProfile() {
        com.neullabs.regulus.compliance.EventCompactionPolicy noErasure =
                new com.neullabs.regulus.compliance.EventCompactionPolicy(
                        Duration.ofDays(365), Duration.ofDays(365 * 7), false);
        assertThatThrownBy(() -> RegulusFirestoreMemoryService.wrap(
                "my-project", "europe-west2", UK_ONLY, noErasure))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("erasure");
    }

    @Test
    void firestoreMemoryAcceptsErasureProfile() {
        RegulusFirestoreMemoryService svc = RegulusFirestoreMemoryService.wrap(
                "my-project", "europe-west2", UK_ONLY, DEFAULT_RETENTION);
        assertThat(svc.databaseLocation()).isEqualTo("europe-west2");
    }

    @Test
    void gcsArtifactRefusesNonAllowlistBucketLocation() {
        assertThatThrownBy(() -> RegulusGcsArtifactService.wrap(
                "my-bucket", "us-central1", null, UK_ONLY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void gcsArtifactRequiresCmekWhenMandated() {
        assertThatThrownBy(() -> RegulusGcsArtifactService.wrap(
                "my-bucket", "europe-west2", null, UK_WITH_CMEK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CMEK");
    }
}
