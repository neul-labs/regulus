package com.neullabs.regulus.adk.plugins.guard;

import com.neullabs.regulus.adk.plugins.PolicyDecision;
import com.neullabs.regulus.identity.Claims;
import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.IdentityHolder;
import com.neullabs.regulus.identity.Jurisdiction;
import com.neullabs.regulus.identity.Principal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegulusIdentityExpiryGuardTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-22T12:00:00Z");

    @AfterEach
    void tearDown() {
        IdentityHolder.clear();
    }

    private Identity identityWithExpiry(Instant expiry) {
        return new Identity(
                new Principal("u-1", "Alice", Principal.PrincipalType.HUMAN),
                new Claims("acme", Jurisdiction.UK, Set.of(), Set.of(), Set.of(), Map.of()),
                new Identity.Provenance("oidc", FIXED_NOW.minusSeconds(60), expiry, "https://idp"));
    }

    @Test
    void absentIdentityIsPermitted() {
        var guard = new RegulusIdentityExpiryGuard(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        assertThat(guard.evaluate()).isEmpty();
    }

    @Test
    void liveTokenIsPermitted() {
        IdentityHolder.set(identityWithExpiry(FIXED_NOW.plusSeconds(60)));
        var guard = new RegulusIdentityExpiryGuard(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        assertThat(guard.evaluate()).isEmpty();
    }

    @Test
    void nullExpiryIsPermitted() {
        IdentityHolder.set(identityWithExpiry(null));
        var guard = new RegulusIdentityExpiryGuard(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        assertThat(guard.evaluate()).isEmpty();
    }

    @Test
    void expiredTokenBlocksWithStructuredReason() {
        IdentityHolder.set(identityWithExpiry(FIXED_NOW.minusSeconds(1)));
        var guard = new RegulusIdentityExpiryGuard(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));

        Optional<PolicyDecision.Block> decision = guard.evaluate();
        assertThat(decision).isPresent();
        assertThat(decision.get().code()).isEqualTo("regulus.identity.token_expired");
        assertThat(decision.get().reason()).contains("expired at");
        assertThat(decision.get().clauseCitation()).isEqualTo("regulus.security.identity_expiry");
    }
}
