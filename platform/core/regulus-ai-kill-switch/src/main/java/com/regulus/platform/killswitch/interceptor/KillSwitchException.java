package com.regulus.platform.killswitch.interceptor;

import com.regulus.platform.killswitch.model.KillSwitchState;

/**
 * Exception thrown when an operation is blocked by the kill switch.
 */
public class KillSwitchException extends RuntimeException {

    private final KillSwitchState state;

    public KillSwitchException(KillSwitchState state) {
        super(formatMessage(state));
        this.state = state;
    }

    public KillSwitchState getState() {
        return state;
    }

    private static String formatMessage(KillSwitchState state) {
        StringBuilder sb = new StringBuilder("AI operations are currently disabled");

        if (state.scope() != null) {
            sb.append(" (scope: ").append(state.scope()).append(")");
        }

        if (state.reason() != null) {
            sb.append(": ").append(state.reason());
        }

        if (state.activatedBy() != null) {
            sb.append(" [activated by: ").append(state.activatedBy()).append("]");
        }

        return sb.toString();
    }
}
