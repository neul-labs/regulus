package com.regulus.platform.killswitch.interceptor;

import com.regulus.platform.killswitch.model.KillSwitchEvent;
import com.regulus.platform.killswitch.model.KillSwitchState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Kill Switch Manager")
class KillSwitchManagerTest {

    private KillSwitchManager manager;
    private ApplicationEventPublisher eventPublisher;
    private KillSwitchStateProvider stateProvider;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        stateProvider = new InMemoryKillSwitchStateProvider();
        manager = new KillSwitchManager(eventPublisher, stateProvider);
    }

    @Nested
    @DisplayName("initial state")
    class InitialState {

        @Test
        @DisplayName("should start with inactive global state")
        void shouldStartInactive() {
            KillSwitchState state = manager.getGlobalState();

            assertThat(state.isActive()).isFalse();
        }

        @Test
        @DisplayName("should have no scoped states initially")
        void shouldHaveNoScopedStates() {
            assertThat(manager.getScopedStates()).isEmpty();
        }
    }

    @Nested
    @DisplayName("global kill switch")
    class GlobalKillSwitch {

        @Test
        @DisplayName("should activate global kill switch")
        void shouldActivateGlobal() {
            manager.activateGlobal("Security incident", "admin@bank.com");

            KillSwitchState state = manager.getGlobalState();
            assertThat(state.isActive()).isTrue();
            assertThat(state.reason()).isEqualTo("Security incident");
            assertThat(state.activatedBy()).isEqualTo("admin@bank.com");
            assertThat(state.scope()).isEqualTo(KillSwitchState.Scope.GLOBAL);
        }

        @Test
        @DisplayName("should publish activation event")
        void shouldPublishActivationEvent() {
            manager.activateGlobal("Test", "tester");

            verify(eventPublisher).publishEvent(any(KillSwitchEvent.class));
        }

        @Test
        @DisplayName("should deactivate global kill switch")
        void shouldDeactivateGlobal() {
            manager.activateGlobal("Test", "admin");
            manager.deactivateGlobal("admin");

            assertThat(manager.getGlobalState().isActive()).isFalse();
        }

        @Test
        @DisplayName("should publish deactivation event")
        void shouldPublishDeactivationEvent() {
            manager.activateGlobal("Test", "admin");
            reset(eventPublisher);

            manager.deactivateGlobal("admin");

            verify(eventPublisher).publishEvent(any(KillSwitchEvent.class));
        }
    }

    @Nested
    @DisplayName("scoped kill switch")
    class ScopedKillSwitch {

        @Test
        @DisplayName("should activate agent-scoped kill switch")
        void shouldActivateAgentScoped() {
            manager.activateScoped(
                KillSwitchState.Scope.AGENT,
                "payment-agent",
                "Agent malfunction",
                "admin"
            );

            assertThat(manager.getScopedStates()).containsKey("agent:payment-agent");
            assertThat(manager.getScopedStates().get("agent:payment-agent").isActive()).isTrue();
        }

        @Test
        @DisplayName("should activate model-scoped kill switch")
        void shouldActivateModelScoped() {
            manager.activateScoped(
                KillSwitchState.Scope.MODEL,
                "gpt-4o",
                "Model issues",
                "admin"
            );

            assertThat(manager.getScopedStates()).containsKey("model:gpt-4o");
        }

        @Test
        @DisplayName("should deactivate scoped kill switch")
        void shouldDeactivateScoped() {
            manager.activateScoped(KillSwitchState.Scope.AGENT, "test-agent", "Test", "admin");
            manager.deactivateScoped(KillSwitchState.Scope.AGENT, "test-agent", "admin");

            assertThat(manager.getScopedStates()).doesNotContainKey("agent:test-agent");
        }
    }

    @Nested
    @DisplayName("blocking checks")
    class BlockingChecks {

        @Test
        @DisplayName("should block when global kill switch active")
        void shouldBlockOnGlobalActive() {
            manager.activateGlobal("Emergency", "admin");

            boolean blocked = manager.isBlocked("any-agent", "any-model", "any-tool");

            assertThat(blocked).isTrue();
        }

        @Test
        @DisplayName("should not block when global inactive")
        void shouldNotBlockWhenInactive() {
            boolean blocked = manager.isBlocked("agent", "model", "tool");

            assertThat(blocked).isFalse();
        }

        @Test
        @DisplayName("should block specific agent when scoped")
        void shouldBlockSpecificAgent() {
            manager.activateScoped(KillSwitchState.Scope.AGENT, "blocked-agent", "Test", "admin");

            assertThat(manager.isBlocked("blocked-agent", null, null)).isTrue();
            assertThat(manager.isBlocked("other-agent", null, null)).isFalse();
        }

        @Test
        @DisplayName("should block specific model when scoped")
        void shouldBlockSpecificModel() {
            manager.activateScoped(KillSwitchState.Scope.MODEL, "gpt-4o", "Model issues", "admin");

            assertThat(manager.isBlocked(null, "gpt-4o", null)).isTrue();
            assertThat(manager.isBlocked(null, "gpt-3.5-turbo", null)).isFalse();
        }

        @Test
        @DisplayName("should block specific tool when scoped")
        void shouldBlockSpecificTool() {
            manager.activateScoped(KillSwitchState.Scope.TOOL, "payment-tool", "Tool bug", "admin");

            assertThat(manager.isBlocked(null, null, "payment-tool")).isTrue();
            assertThat(manager.isBlocked(null, null, "other-tool")).isFalse();
        }

        @Test
        @DisplayName("should return blocking state")
        void shouldReturnBlockingState() {
            manager.activateGlobal("Emergency shutdown", "admin");

            KillSwitchState state = manager.getBlockingState("agent", "model", "tool");

            assertThat(state).isNotNull();
            assertThat(state.isActive()).isTrue();
            assertThat(state.reason()).isEqualTo("Emergency shutdown");
        }

        @Test
        @DisplayName("should return null when not blocked")
        void shouldReturnNullWhenNotBlocked() {
            KillSwitchState state = manager.getBlockingState("agent", "model", "tool");

            assertThat(state).isNull();
        }
    }

    @Nested
    @DisplayName("state persistence")
    class StatePersistence {

        @Test
        @DisplayName("should save state to provider on activation")
        void shouldSaveStateOnActivation() {
            manager.activateGlobal("Test", "admin");

            var states = stateProvider.loadAllStates();
            assertThat(states).containsKey("global");
        }

        @Test
        @DisplayName("should remove state from provider on deactivation")
        void shouldRemoveStateOnDeactivation() {
            manager.activateGlobal("Test", "admin");
            manager.deactivateGlobal("admin");

            var states = stateProvider.loadAllStates();
            assertThat(states).doesNotContainKey("global");
        }
    }
}
