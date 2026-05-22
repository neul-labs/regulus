package com.neullabs.regulus.identity.spi;

/**
 * Thrown by an {@link IdentityAdapter} when it refuses to mint an Identity
 * for a request. Checked so callers cannot accidentally swallow it.
 */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
