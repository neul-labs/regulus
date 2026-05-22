package com.neullabs.regulus.adk.a2a.signing;

import java.util.Map;
import java.util.Objects;

/**
 * The result of signing an {@link A2AEnvelope}. Carries the original
 * envelope, the headers that need to be added to the wire request
 * ({@code Signature}, {@code Signature-Input}, {@code Content-Digest},
 * {@code Regulus-Tenant}, {@code Regulus-Correlation-Id}), and the key id
 * used so verifiers can look up the matching {@code VerificationKey}.
 */
public record SignedEnvelope(
        A2AEnvelope envelope,
        Map<String, String> signatureHeaders,
        String keyId) {

    public SignedEnvelope {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(signatureHeaders, "signatureHeaders");
        Objects.requireNonNull(keyId, "keyId");
        signatureHeaders = Map.copyOf(signatureHeaders);
    }
}
