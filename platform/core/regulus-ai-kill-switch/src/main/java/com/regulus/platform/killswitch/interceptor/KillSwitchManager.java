package com.regulus.platform.killswitch.interceptor;

import com.regulus.platform.killswitch.model.KillSwitchEvent;
import com.regulus.platform.killswitch.model.KillSwitchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central manager for kill switch state and operations.
 * Supports global and scoped kill switches with audit logging.
 */
public class KillSwitchManager {

    private static final Logger log = LoggerFactory.getLogger(KillSwitchManager.class);

    private final AtomicReference<KillSwitchState> globalState;
    private final Map<String, KillSwitchState> scopedStates;
    private final ApplicationEventPublisher eventPublisher;
    private final KillSwitchStateProvider stateProvider;

    public KillSwitchManager(ApplicationEventPublisher eventPublisher,
                             KillSwitchStateProvider stateProvider) {
        this.eventPublisher = eventPublisher;
        this.stateProvider = stateProvider;
        this.globalState = new AtomicReference<>(KillSwitchState.inactive());
        this.scopedStates = new ConcurrentHashMap<>();

        // Initialize from external state provider
        refreshState();
    }

    /**
     * Check if any kill switch is active that would block the given context.
     */
    public boolean isBlocked(String agentId, String modelId, String toolId) {
        // Check global kill switch
        if (globalState.get().isActive()) {
            return true;
        }

        // Check scoped kill switches
        if (agentId != null && isAgentBlocked(agentId)) {
            return true;
        }
        if (modelId != null && isModelBlocked(modelId)) {
            return true;
        }
        if (toolId != null && isToolBlocked(toolId)) {
            return true;
        }

        return false;
    }

    /**
     * Get the current blocking state, if any.
     */
    public KillSwitchState getBlockingState(String agentId, String modelId, String toolId) {
        if (globalState.get().isActive()) {
            return globalState.get();
        }

        if (agentId != null) {
            KillSwitchState state = scopedStates.get("agent:" + agentId);
            if (state != null && state.isActive()) {
                return state;
            }
        }

        if (modelId != null) {
            KillSwitchState state = scopedStates.get("model:" + modelId);
            if (state != null && state.isActive()) {
                return state;
            }
        }

        if (toolId != null) {
            KillSwitchState state = scopedStates.get("tool:" + toolId);
            if (state != null && state.isActive()) {
                return state;
            }
        }

        return null;
    }

    /**
     * Activate the global kill switch.
     */
    public void activateGlobal(String reason, String activatedBy) {
        KillSwitchState newState = KillSwitchState.builder()
            .activated(true)
            .reason(reason)
            .activatedBy(activatedBy)
            .activatedAt(Instant.now())
            .scope(KillSwitchState.Scope.GLOBAL)
            .build();

        KillSwitchState previous = globalState.getAndSet(newState);

        log.warn("KILL SWITCH ACTIVATED - Global. reason='{}', activatedBy='{}'",
            reason, activatedBy);

        publishEvent(KillSwitchEvent.activated(newState));

        // Persist to external provider
        if (stateProvider != null) {
            stateProvider.saveState("global", newState);
        }
    }

    /**
     * Activate a scoped kill switch.
     */
    public void activateScoped(KillSwitchState.Scope scope, String targetId,
                               String reason, String activatedBy) {
        KillSwitchState newState = KillSwitchState.builder()
            .activated(true)
            .reason(reason)
            .activatedBy(activatedBy)
            .activatedAt(Instant.now())
            .scope(scope)
            .build();

        String key = scope.name().toLowerCase() + ":" + targetId;
        scopedStates.put(key, newState);

        log.warn("KILL SWITCH ACTIVATED - {}:{}. reason='{}', activatedBy='{}'",
            scope, targetId, reason, activatedBy);

        publishEvent(KillSwitchEvent.activated(newState));

        if (stateProvider != null) {
            stateProvider.saveState(key, newState);
        }
    }

    /**
     * Deactivate the global kill switch.
     */
    public void deactivateGlobal(String deactivatedBy) {
        KillSwitchState previous = globalState.getAndSet(KillSwitchState.inactive());

        if (previous.isActive()) {
            log.info("KILL SWITCH DEACTIVATED - Global. deactivatedBy='{}'", deactivatedBy);
            publishEvent(KillSwitchEvent.deactivated(previous));

            if (stateProvider != null) {
                stateProvider.removeState("global");
            }
        }
    }

    /**
     * Deactivate a scoped kill switch.
     */
    public void deactivateScoped(KillSwitchState.Scope scope, String targetId,
                                 String deactivatedBy) {
        String key = scope.name().toLowerCase() + ":" + targetId;
        KillSwitchState previous = scopedStates.remove(key);

        if (previous != null && previous.isActive()) {
            log.info("KILL SWITCH DEACTIVATED - {}:{}. deactivatedBy='{}'",
                scope, targetId, deactivatedBy);
            publishEvent(KillSwitchEvent.deactivated(previous));

            if (stateProvider != null) {
                stateProvider.removeState(key);
            }
        }
    }

    /**
     * Refresh state from external provider (e.g., Vault, ConfigHub).
     */
    public void refreshState() {
        if (stateProvider == null) {
            return;
        }

        try {
            Map<String, KillSwitchState> externalStates = stateProvider.loadAllStates();

            if (externalStates.containsKey("global")) {
                globalState.set(externalStates.get("global"));
            }

            for (Map.Entry<String, KillSwitchState> entry : externalStates.entrySet()) {
                if (!entry.getKey().equals("global")) {
                    scopedStates.put(entry.getKey(), entry.getValue());
                }
            }

            log.debug("Kill switch state refreshed from provider");
        } catch (Exception e) {
            log.error("Failed to refresh kill switch state from provider", e);
        }
    }

    public KillSwitchState getGlobalState() {
        return globalState.get();
    }

    public Map<String, KillSwitchState> getScopedStates() {
        return Map.copyOf(scopedStates);
    }

    private boolean isAgentBlocked(String agentId) {
        KillSwitchState state = scopedStates.get("agent:" + agentId);
        return state != null && state.isActive();
    }

    private boolean isModelBlocked(String modelId) {
        KillSwitchState state = scopedStates.get("model:" + modelId);
        return state != null && state.isActive();
    }

    private boolean isToolBlocked(String toolId) {
        KillSwitchState state = scopedStates.get("tool:" + toolId);
        return state != null && state.isActive();
    }

    private void publishEvent(KillSwitchEvent event) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
    }
}
