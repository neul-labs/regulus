package com.neullabs.regulus.killswitch.authz;

import com.neullabs.regulus.identity.Claims;
import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.Jurisdiction;
import com.neullabs.regulus.identity.Principal;
import com.neullabs.regulus.killswitch.model.KillSwitchState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoleBasedKillSwitchAuthorizerTest {

    private Identity identityWithRoles(Set<String> roles) {
        return new Identity(
                new Principal("u-1", "Alice", Principal.PrincipalType.HUMAN),
                new Claims("acme", Jurisdiction.UK, Set.of(), roles, Set.of(), Map.of()),
                new Identity.Provenance("oidc", Instant.now(), null, "https://idp.example"));
    }

    @Test
    void canRequestRequiresRequesterRole() {
        var authz = new RoleBasedKillSwitchAuthorizer();
        var with = identityWithRoles(Set.of("regulus.killswitch.requester"));
        var without = identityWithRoles(Set.of("agent-operator"));

        assertThat(authz.canRequest(with, KillSwitchState.Scope.GLOBAL)).isTrue();
        assertThat(authz.canRequest(without, KillSwitchState.Scope.GLOBAL)).isFalse();
        assertThat(authz.canRequest(null, KillSwitchState.Scope.GLOBAL)).isFalse();
    }

    @Test
    void canApproveAndEmergencyBypassUseTheirOwnRoles() {
        var authz = new RoleBasedKillSwitchAuthorizer();
        var approver = identityWithRoles(Set.of("regulus.killswitch.approver"));
        var emergency = identityWithRoles(Set.of("regulus.killswitch.emergency"));
        var neither = identityWithRoles(Set.of("regulus.killswitch.requester"));

        assertThat(authz.canApprove(approver, KillSwitchState.Scope.GLOBAL)).isTrue();
        assertThat(authz.canApprove(neither, KillSwitchState.Scope.GLOBAL)).isFalse();
        assertThat(authz.canEmergencyBypass(emergency)).isTrue();
        assertThat(authz.canEmergencyBypass(neither)).isFalse();
    }

    @Test
    void customRoleNamesAreUsed() {
        var authz = new RoleBasedKillSwitchAuthorizer("ops.request", "ops.approve", "ops.911");
        var requester = identityWithRoles(Set.of("ops.request"));

        assertThat(authz.canRequest(requester, KillSwitchState.Scope.GLOBAL)).isTrue();
        // Canonical role should NOT work under custom config
        var canonical = identityWithRoles(Set.of("regulus.killswitch.requester"));
        assertThat(authz.canRequest(canonical, KillSwitchState.Scope.GLOBAL)).isFalse();
    }
}
