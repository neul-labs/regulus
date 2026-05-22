package com.neullabs.regulus.adk.a2a.signing;

import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.crypto.KeyProvider;

import java.time.Duration;

/**
 * RFC 9421 (HTTP Message Signatures) implementation of {@link A2ARequestSigner}
 * using Ed25519 keys provided by a {@link KeyProvider}.
 *
 * <p>The signature base covers (in order):
 * <ol>
 *   <li>{@code "@method"} — request method</li>
 *   <li>{@code "@target-uri"} — full target URI</li>
 *   <li>{@code "content-digest"} — SHA-256 of the body</li>
 *   <li>{@code "regulus-tenant"} — caller's tenant id</li>
 *   <li>{@code "regulus-correlation-id"} — request correlation id</li>
 *   <li>{@code "regulus-identity-adapter"} — adapter that minted the caller</li>
 * </ol>
 *
 * <p>Replay protection: each signature includes a unique {@code nonce} param
 * and a {@code created} timestamp; verifiers reject nonces seen within the
 * {@link #replayWindow} and timestamps drifting more than the window from
 * local time.
 *
 * <p>This skeleton wires the SPI plumbing. The signing/verification body
 * (canonicalisation, Ed25519 sign/verify, nonce cache) is intentionally left
 * unimplemented and tracked as a follow-up — the goal of this commit is to
 * lock in the contract so callers can be built against a stable surface.
 */
public final class HttpMessageSignatureSigner implements A2ARequestSigner {

    private final KeyProvider keyProvider;
    private final Duration replayWindow;

    public HttpMessageSignatureSigner(KeyProvider keyProvider, Duration replayWindow) {
        this.keyProvider = keyProvider;
        this.replayWindow = replayWindow == null ? Duration.ofMinutes(5) : replayWindow;
    }

    public HttpMessageSignatureSigner(KeyProvider keyProvider) {
        this(keyProvider, Duration.ofMinutes(5));
    }

    public KeyProvider keyProvider() { return keyProvider; }

    public Duration replayWindow() { return replayWindow; }

    @Override
    public SignedEnvelope sign(A2AEnvelope envelope, Identity caller) {
        throw new UnsupportedOperationException(
                "HttpMessageSignatureSigner.sign(): RFC 9421 canonicalisation + Ed25519 sign "
                        + "is the next implementation milestone. SPI surface is locked.");
    }

    @Override
    public VerifiedCaller verify(SignedEnvelope envelope) throws SignatureException {
        throw new SignatureException(
                "HttpMessageSignatureSigner.verify(): RFC 9421 canonicalisation + Ed25519 verify "
                        + "is the next implementation milestone. SPI surface is locked.");
    }
}
