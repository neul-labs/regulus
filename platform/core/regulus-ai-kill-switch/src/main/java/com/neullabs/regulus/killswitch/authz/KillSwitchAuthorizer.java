package com.neullabs.regulus.killswitch.authz;

import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.killswitch.model.KillSwitchState;

/**
 * Decides which Identities are permitted to request, approve, or emergency
 * bypass kill-switch operations. Implementations should be pure — no
 * side-effects, no audit emissions (the surrounding {@code DualControlKillSwitch}
 * handles audit on the boundary).
 */
public interface KillSwitchAuthorizer {

    boolean canRequest(Identity identity, KillSwitchState.Scope scope);

    boolean canApprove(Identity identity, KillSwitchState.Scope scope);

    boolean canEmergencyBypass(Identity identity);
}
