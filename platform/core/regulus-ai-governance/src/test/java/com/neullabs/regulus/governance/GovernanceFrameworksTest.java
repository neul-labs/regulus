package com.neullabs.regulus.governance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GovernanceFrameworksTest {

    @Test
    void allCatalogueIdsResolve() {
        for (String id : GovernanceFrameworks.all().keySet()) {
            assertThat(GovernanceFrameworks.byId(id))
                    .as("framework id: %s", id)
                    .isNotNull()
                    .extracting(GovernanceFramework::id)
                    .isEqualTo(id);
        }
    }

    @Test
    void unknownIdThrows() {
        assertThatThrownBy(() -> GovernanceFrameworks.byId("does-not-exist"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does-not-exist");
    }

    @Test
    void composeWraps() {
        GovernanceFramework composite = GovernanceFrameworks.compose(
                List.of("nist-ai-rmf", "iso-42001"));
        assertThat(composite).isInstanceOf(CompositeGovernanceFramework.class);
    }

    @Test
    void catalogueContainsExpectedFrameworks() {
        assertThat(GovernanceFrameworks.all().keySet()).containsExactlyInAnyOrder(
                "nist-ai-rmf", "nist-ai-rmf-600-1", "nist-ai-rmf-agent-interop",
                "iso-42001", "iso-23894", "iso-23053");
    }
}
