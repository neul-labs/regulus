package com.neullabs.regulus.identity.crypto;

/**
 * Opaque handle to a public verification key (or certificate). Mirrors the
 * {@link SigningKey} pattern so the {@link KeyProvider} can hand either
 * side of the keypair to signers/verifiers without leaking concrete crypto
 * types into module boundaries.
 */
public sealed interface VerificationKey permits VerificationKey.Handle {

    String keyId();

    String algorithm();

    record Handle(String keyId, String algorithm, Object material) implements VerificationKey {}
}
