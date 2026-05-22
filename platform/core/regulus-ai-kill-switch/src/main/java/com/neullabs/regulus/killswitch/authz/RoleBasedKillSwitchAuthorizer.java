package com.neullabs.regulus.killswitch.authz;

import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.killswitch.model.KillSwitchState;

import java.util.Objects;

/**
 * Default {@link KillSwitchAuthorizer}. Permits an Identity to request,
 * approve, or emergency-bypass when its {@code Claims.roles()} contain the
 * configured role name. Role names default to the canonical
 * {@code regulus.killswitch.*} set and are overridable per tenant.
 */
public final class RoleBasedKillSwitchAuthorizer implements KillSwitchAuthorizer {

    public static final String DEFAULT_REQUESTER_ROLE = "regulus.killswitch.requester";
    public static final String DEFAULT_APPROVER_ROLE = "regulus.killswitch.approver";
    public static final String DEFAULT_EMERGENCY_ROLE = "regulus.killswitch.emergency";

    private final String requesterRole;
    private final String approverRole;
    private final String emergencyRole;

    public RoleBasedKillSwitchAuthorizer() {
        this(DEFAULT_REQUESTER_ROLE, DEFAULT_APPROVER_ROLE, DEFAULT_EMERGENCY_ROLE);
    }

    public RoleBasedKillSwitchAuthorizer(String requesterRole, String approverRole, String emergencyRole) {
        this.requesterRole = Objects.requireNonNull(requesterRole, "requesterRole");
        this.approverRole = Objects.requireNonNull(approverRole, "approverRole");
        this.emergencyRole = Objects.requireNonNull(emergencyRole, "emergencyRole");
    }

    @Override
    public boolean canRequest(Identity identity, KillSwitchState.Scope scope) {
        return identity != null && identity.claims().hasRole(requesterRole);
    }

    @Override
    public boolean canApprove(Identity identity, KillSwitchState.Scope scope) {
        return identity != null && identity.claims().hasRole(approverRole);
    }

    @Override
    public boolean canEmergencyBypass(Identity identity) {
        return identity != null && identity.claims().hasRole(emergencyRole);
    }

    public String requesterRole() { return requesterRole; }
    public String approverRole() { return approverRole; }
    public String emergencyRole() { return emergencyRole; }
}
