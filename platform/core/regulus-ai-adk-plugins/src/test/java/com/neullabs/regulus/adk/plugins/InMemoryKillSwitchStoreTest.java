package com.neullabs.regulus.adk.plugins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryKillSwitchStoreTest {

    private InMemoryKillSwitchStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryKillSwitchStore();
    }

    @Test
    void activateMakesScopeActive() {
        assertThat(store.isActive("agents/x")).isFalse();
        store.activate("agents/x", "ops-A", "drill");
        assertThat(store.isActive("agents/x")).isTrue();
    }

    @Test
    void deactivateRequestDoesNotImmediatelyDeactivate() {
        store.activate("agents/x", "ops-A", "drill");
        boolean immediate = store.requestDeactivate("agents/x", "ops-B", "drill cleanup");
        assertThat(immediate).isFalse();
        assertThat(store.isActive("agents/x")).isTrue();
    }

    @Test
    void confirmDeactivateLiftsTheSwitch() {
        store.activate("agents/x", "ops-A", "drill");
        store.requestDeactivate("agents/x", "ops-B", "drill cleanup");
        store.confirmDeactivate("agents/x", "ops-C");
        assertThat(store.isActive("agents/x")).isFalse();
    }

    @Test
    void sameOperatorCannotBothRequestAndConfirm() {
        store.activate("agents/x", "ops-A", "drill");
        store.requestDeactivate("agents/x", "ops-B", "drill cleanup");
        assertThatThrownBy(() -> store.confirmDeactivate("agents/x", "ops-B"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Confirming operator must differ");
    }

    @Test
    void confirmWithoutPendingRequestThrows() {
        store.activate("agents/x", "ops-A", "drill");
        assertThatThrownBy(() -> store.confirmDeactivate("agents/x", "ops-B"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No pending deactivation");
    }

    @Test
    void requestOnInactiveScopeReturnsTrue() {
        // Nothing to deactivate; the call is a no-op that returns "applied".
        assertThat(store.requestDeactivate("agents/x", "ops-B", "noop")).isTrue();
    }
}
