package com.neullabs.regulus.governance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeGovernanceFrameworkTest {

    @Test
    void emptyCompositeRejected() {
        assertThatThrownBy(() -> new CompositeGovernanceFramework(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compositeKindIsStrongest() {
        // nist-ai-rmf = VOLUNTARY, iso-42001 = CERTIFIABLE → composite is CERTIFIABLE.
        GovernanceFramework composite = GovernanceFrameworks.compose(
                List.of("nist-ai-rmf", "iso-42001"));
        assertThat(composite.kind()).isEqualTo(FrameworkKind.CERTIFIABLE);
    }

    @Test
    void controlsUnioned() {
        GovernanceFramework composite = GovernanceFrameworks.compose(
                List.of("nist-ai-rmf", "iso-42001"));
        var nistControls = GovernanceFrameworks.byId("nist-ai-rmf").controls();
        var isoControls = GovernanceFrameworks.byId("iso-42001").controls();
        assertThat(composite.controls()).containsAll(nistControls);
        assertThat(composite.controls()).containsAll(isoControls);
    }

    @Test
    void bindingsUnioned() {
        GovernanceFramework composite = GovernanceFrameworks.compose(
                List.of("nist-ai-rmf", "iso-42001"));
        var nistBindings = GovernanceFrameworks.byId("nist-ai-rmf").bindings();
        var isoBindings = GovernanceFrameworks.byId("iso-42001").bindings();
        assertThat(composite.bindings()).containsAll(nistBindings);
        assertThat(composite.bindings()).containsAll(isoBindings);
    }

    @Test
    void componentsAccessible() {
        CompositeGovernanceFramework composite = (CompositeGovernanceFramework)
                GovernanceFrameworks.compose(List.of("nist-ai-rmf", "iso-42001"));
        assertThat(composite.components())
                .hasSize(2)
                .extracting(GovernanceFramework::id)
                .containsExactly("nist-ai-rmf", "iso-42001");
    }
}
