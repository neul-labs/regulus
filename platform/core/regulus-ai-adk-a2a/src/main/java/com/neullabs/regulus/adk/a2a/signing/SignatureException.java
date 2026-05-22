package com.neullabs.regulus.adk.a2a.signing;

/**
 * Thrown when an inbound {@link SignedEnvelope} fails verification: the
 * signature doesn't validate, the {@code Content-Digest} doesn't match the
 * body, the nonce was seen recently, or the timestamp is outside the replay
 * window. Checked so callers can't accidentally swallow it.
 */
public class SignatureException extends Exception {

    public SignatureException(String message) {
        super(message);
    }

    public SignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
