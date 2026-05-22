package com.neullabs.regulus.adk.a2a.signing;

import com.neullabs.regulus.identity.Identity;

import java.time.Instant;
import java.util.Objects;

/**
 * What {@code verify()} hands back to the inbound A2A pipeline: the caller's
 * {@link Identity} (reconstructed from the verified envelope claims) and the
 * timestamp the signature commits to (used downstream for replay-window
 * checks).
 */
public record VerifiedCaller(Identity identity, Instant signedAt, String keyId) {

    public VerifiedCaller {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(signedAt, "signedAt");
        Objects.requireNonNull(keyId, "keyId");
    }
}
