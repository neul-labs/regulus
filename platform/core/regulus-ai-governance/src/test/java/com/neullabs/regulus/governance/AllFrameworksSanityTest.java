package com.neullabs.regulus.governance;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AllFrameworksSanityTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "nist-ai-rmf", "nist-ai-rmf-600-1", "nist-ai-rmf-agent-interop",
            "iso-42001", "iso-23894", "iso-23053"
    })
    void idMatchesItself(String id) {
        assertThat(GovernanceFrameworks.byId(id).id()).isEqualTo(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "nist-ai-rmf", "nist-ai-rmf-600-1", "nist-ai-rmf-agent-interop",
            "iso-42001", "iso-23894", "iso-23053"
    })
    void displayNameIsNonEmpty(String id) {
        assertThat(GovernanceFrameworks.byId(id).displayName()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "nist-ai-rmf", "nist-ai-rmf-600-1", "nist-ai-rmf-agent-interop",
            "iso-42001", "iso-23894", "iso-23053"
    })
    void controlsAreNonEmpty(String id) {
        assertThat(GovernanceFrameworks.byId(id).controls()).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "nist-ai-rmf", "nist-ai-rmf-600-1", "nist-ai-rmf-agent-interop",
            "iso-42001", "iso-23894", "iso-23053"
    })
    void bindingsAreNonEmpty(String id) {
        assertThat(GovernanceFrameworks.byId(id).bindings()).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "nist-ai-rmf", "nist-ai-rmf-600-1", "nist-ai-rmf-agent-interop",
            "iso-42001", "iso-23894", "iso-23053"
    })
    void bindingControlIdsAppearInControls(String id) {
        GovernanceFramework framework = GovernanceFrameworks.byId(id);
        var declaredControlIds = framework.controls().stream()
                .map(FrameworkControl::id).toList();
        for (FrameworkBinding binding : framework.bindings()) {
            assertThat(declaredControlIds)
                    .as("binding for %s references controlId=%s",
                            id, binding.controlId())
                    .contains(binding.controlId());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "nist-ai-rmf", "nist-ai-rmf-600-1", "nist-ai-rmf-agent-interop",
            "iso-42001", "iso-23894", "iso-23053"
    })
    void authorityUrlIsHttp(String id) {
        assertThat(GovernanceFrameworks.byId(id).authorityUrl())
                .startsWithIgnoringCase("http");
    }
}
