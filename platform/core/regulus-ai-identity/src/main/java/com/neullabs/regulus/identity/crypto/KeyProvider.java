package com.neullabs.regulus.identity.crypto;

/**
 * Source of signing and verification key material. The same interface is
 * consumed by A2A request signing (RFC 9421) and opt-in audit chain
 * signing, so a tenant only configures key management once.
 *
 * <p>Concrete adapters (JCA in-process, GCP KMS, HashiCorp Vault transit)
 * live in separate starters and are not part of this leaf module.
 */
public interface KeyProvider {

    SigningKey signingKey(String keyId);

    VerificationKey verificationKey(String keyId);

    String defaultSigningKeyId();
}
