package com.neullabs.regulus.adk.plugins.guard;

import com.google.adk.plugins.BasePlugin;
import com.neullabs.regulus.adk.plugins.PolicyDecision;
import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.IdentityHolder;

import java.time.Clock;
import java.util.Optional;

/**
 * ADK {@code BeforeModelCallback} that refuses to forward the call if the
 * caller's {@link Identity} (bound to {@link IdentityHolder} by an inbound
 * filter) has an expired token. This is the runtime enforcement point for
 * {@link Identity.Provenance#tokenExpiry()} — {@code IdentityHolder.get()}
 * deliberately does <em>not</em> throw, because non-policy callers (such
 * as audit-log enrichment reading historical Identity) would be broken by
 * unconditional expiry rejection.
 *
 * <p>This guard is registered first in the {@code RegulusPolicyPlugin}
 * chain so denied requests never reach purpose-binding or consent checks.
 *
 * <p>Absent-Identity is intentionally <strong>permitted</strong> here —
 * deployments without an inbound IdentityAdapter must still function for
 * internal callers; expiry can only matter when a token is present.
 */
public final class RegulusIdentityExpiryGuard extends BasePlugin {

    public static final String NAME = "regulus-identity-expiry";

    private final Clock clock;

    public RegulusIdentityExpiryGuard() {
        this(Clock.systemUTC());
    }

    public RegulusIdentityExpiryGuard(Clock clock) {
        super(NAME);
        this.clock = clock;
    }

    /**
     * Evaluates the current thread's bound {@link Identity}. Returns
     * {@code Optional.empty()} when the call should proceed; returns a
     * populated {@link PolicyDecision.Block} when the bound Identity is
     * expired. The actual ADK {@code @Override} hook (BeforeModelCallback /
     * BeforeToolCallback) will delegate to this method once ADK 1.2.0
     * signatures are pinned alongside the other Regulus plugins.
     */
    public Optional<PolicyDecision.Block> evaluate() {
        Optional<Identity> bound = IdentityHolder.get();
        if (bound.isEmpty()) {
            return Optional.empty();
        }
        Identity id = bound.get();
        if (id.isExpired(clock.instant())) {
            return Optional.of(new PolicyDecision.Block(
                    "regulus.identity.token_expired",
                    "Caller Identity token expired at " + id.provenance().tokenExpiry()
                            + " (adapter=" + id.provenance().adapterId() + ")",
                    // No specific regulatory clause — this is a Regulus internal precondition
                    "regulus.security.identity_expiry"));
        }
        return Optional.empty();
    }
}
