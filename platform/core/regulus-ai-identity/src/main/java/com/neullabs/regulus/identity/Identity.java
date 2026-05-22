package com.neullabs.regulus.identity;

import java.time.Instant;
import java.util.Objects;

/**
 * The canonical, post-authentication identity bundle. Every adapter mints
 * one of these; every downstream consumer (policy guards, audit, A2A signing,
 * kill-switch authorization) reads only from here.
 *
 * <p>{@link Provenance} carries which adapter produced this Identity, when it
 * was minted, and when the underlying token expires. Expiry enforcement is
 * not done by {@link IdentityHolder#get()} — see
 * {@code RegulusIdentityExpiryGuard} in {@code regulus-ai-adk-plugins} for
 * the runtime gate.
 */
public record Identity(Principal principal, Claims claims, Provenance provenance) {

    public Identity {
        Objects.requireNonNull(principal, "Identity.principal");
        Objects.requireNonNull(claims, "Identity.claims");
        Objects.requireNonNull(provenance, "Identity.provenance");
    }

    public boolean isExpired(Instant now) {
        Instant exp = provenance.tokenExpiry();
        return exp != null && !now.isBefore(exp);
    }

    public record Provenance(String adapterId, Instant mintedAt, Instant tokenExpiry, String tokenIssuer) {

        public Provenance {
            Objects.requireNonNull(adapterId, "Provenance.adapterId");
            Objects.requireNonNull(mintedAt, "Provenance.mintedAt");
        }
    }
}
