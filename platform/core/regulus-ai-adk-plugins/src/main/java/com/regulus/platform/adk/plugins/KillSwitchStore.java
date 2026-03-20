package com.regulus.platform.adk.plugins;

/**
 * State backing a {@link RegulusKillSwitchPlugin}. Implementations must be
 * safe to read concurrently and must support a monotonic activation guarantee
 * (once activated, the switch stays active until explicitly cleared by an
 * authorised dual-control flow).
 */
public interface KillSwitchStore {

    boolean isActive(String scope);

    /** Activate the switch. One operator can do this unilaterally. */
    void activate(String scope, String operator, String reason);

    /**
     * Request deactivation. Returns {@code true} if applied immediately
     * (e.g. in non-dual-control mode), {@code false} if a second operator's
     * confirmation is pending via ADK's {@code ToolConfirmation} flow.
     */
    boolean requestDeactivate(String scope, String operator, String reason);

    /** Confirm a pending deactivation. Operator must differ from the requester. */
    void confirmDeactivate(String scope, String confirmingOperator);
}
