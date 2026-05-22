package com.neullabs.regulus.adk.a2a.signing;

import com.neullabs.regulus.identity.Identity;

/**
 * Signs outbound A2A requests and verifies inbound ones. The default
 * implementation uses RFC 9421 (HTTP Message Signatures) with Ed25519 — it
 * binds method, target URI, body digest, and selected Regulus headers
 * together so an intermediary stripping or rewriting a header invalidates
 * the signature.
 *
 * <p>Key material is fetched through
 * {@link com.neullabs.regulus.identity.crypto.KeyProvider}; the same
 * provider is shared with the audit chain so a tenant configures keys
 * once.
 *
 * <p>Verification reconstructs the caller's {@link Identity} from the
 * envelope's Regulus headers and the matched {@code VerificationKey}; that
 * reconstructed Identity is what the inbound filter places into
 * {@code IdentityHolder} before policy guards run.
 */
public interface A2ARequestSigner {

    /** Produce a signed envelope for the given outbound call. */
    SignedEnvelope sign(A2AEnvelope envelope, Identity caller);

    /**
     * Verify an inbound signed envelope. Must check signature validity,
     * content-digest, nonce uniqueness (replay protection) and the
     * timestamp's drift against the local clock. Returns the verified
     * caller's Identity.
     */
    VerifiedCaller verify(SignedEnvelope envelope) throws SignatureException;
}
