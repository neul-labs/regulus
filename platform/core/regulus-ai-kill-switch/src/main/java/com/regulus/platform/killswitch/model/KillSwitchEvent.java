package com.regulus.platform.killswitch.model;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when kill switch state changes.
 */
public record KillSwitchEvent(
    EventType type,
    KillSwitchState state,
    Instant timestamp,
    Map<String, Object> metadata
) {

    public enum EventType {
        ACTIVATED,
        DEACTIVATED,
        SCOPE_CHANGED,
        HEALTH_CHECK,
        DRILL_STARTED,
        DRILL_COMPLETED
    }

    public static KillSwitchEvent activated(KillSwitchState state) {
        return new KillSwitchEvent(
            EventType.ACTIVATED,
            state,
            Instant.now(),
            Map.of()
        );
    }

    public static KillSwitchEvent deactivated(KillSwitchState previousState) {
        return new KillSwitchEvent(
            EventType.DEACTIVATED,
            KillSwitchState.inactive(),
            Instant.now(),
            Map.of("previousState", previousState)
        );
    }
}
