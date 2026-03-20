package com.regulus.platform.adk.plugins;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local kill-switch state. Suitable for development and tests; real
 * deployments substitute a persistent store via {@link RegulusKillSwitchPlugin#withStore}.
 */
public final class InMemoryKillSwitchStore implements KillSwitchStore {

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    @Override
    public boolean isActive(String scope) {
        State s = states.get(scope);
        return s != null && s.active;
    }

    @Override
    public void activate(String scope, String operator, String reason) {
        states.put(scope, new State(true, operator, reason, null));
    }

    @Override
    public boolean requestDeactivate(String scope, String operator, String reason) {
        State current = states.get(scope);
        if (current == null || !current.active) return true;
        states.put(scope, new State(true, current.activatedBy, current.activationReason, operator));
        return false;
    }

    @Override
    public void confirmDeactivate(String scope, String confirmingOperator) {
        State current = states.get(scope);
        if (current == null || !current.active) return;
        if (current.pendingDeactivateBy == null) {
            throw new IllegalStateException("No pending deactivation request for " + scope);
        }
        if (current.pendingDeactivateBy.equals(confirmingOperator)) {
            throw new IllegalStateException("Confirming operator must differ from requester");
        }
        states.put(scope, new State(false, null, null, null));
    }

    private record State(boolean active, String activatedBy, String activationReason, String pendingDeactivateBy) {}
}
