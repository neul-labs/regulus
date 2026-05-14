package com.neullabs.regulus.governance;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GovernanceProgramStateTest {

    @Test
    void implementationRatioCountsImplementedAndPartial() {
        GovernanceProgramState state = new GovernanceProgramState(
                List.of(GovernanceFrameworks.byId("iso-42001")),
                Map.of(
                        "A.2.2", ControlImplementationStatus.IMPLEMENTED,
                        "A.3.2", ControlImplementationStatus.PARTIAL,
                        "A.6.2.7", ControlImplementationStatus.GAP,
                        "A.10.4", ControlImplementationStatus.NOT_APPLICABLE
                ),
                Optional.empty());
        // 2 implemented/partial out of 3 in-scope (NOT_APPLICABLE excluded) = 0.666...
        assertThat(state.implementationRatio()).isCloseTo(2.0 / 3.0, within(0.001));
    }

    @Test
    void gapsAreListedSorted() {
        GovernanceProgramState state = new GovernanceProgramState(
                List.of(GovernanceFrameworks.byId("iso-42001")),
                Map.of(
                        "A.6.2.7", ControlImplementationStatus.GAP,
                        "A.2.2", ControlImplementationStatus.GAP,
                        "A.10.4", ControlImplementationStatus.IMPLEMENTED
                ),
                Optional.empty());
        assertThat(state.gaps()).containsExactly("A.2.2", "A.6.2.7");
    }

    @Test
    void implementationRatioZeroWhenAllNotApplicable() {
        GovernanceProgramState state = new GovernanceProgramState(
                List.of(GovernanceFrameworks.byId("iso-42001")),
                Map.of(
                        "A.2.2", ControlImplementationStatus.NOT_APPLICABLE,
                        "A.10.4", ControlImplementationStatus.NOT_APPLICABLE
                ),
                Optional.empty());
        assertThat(state.implementationRatio()).isEqualTo(0.0);
    }
}
