package com.neullabs.regulus.identity.crypto;

/**
 * Opaque handle to a private signing key. Implementations may wrap an
 * in-process JCA {@code PrivateKey}, a remote KMS key reference, or a
 * Vault transit key. Consumers never inspect the key material directly —
 * they pass the handle to a signer SPI that knows how to invoke the
 * underlying provider.
 */
public sealed interface SigningKey permits SigningKey.Handle {

    String keyId();

    String algorithm();

    record Handle(String keyId, String algorithm, Object material) implements SigningKey {}
}
