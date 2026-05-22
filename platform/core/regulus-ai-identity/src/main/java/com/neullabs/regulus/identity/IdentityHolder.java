package com.neullabs.regulus.identity;

import java.util.Optional;

/**
 * Thread-local current {@link Identity}. Adapters call {@link #set(Identity)}
 * at request entry (typically from a Servlet filter or A2A pre-dispatch hook);
 * the request thread's plugins, audit emitters and signers read it. Always
 * pair {@code set}/{@code clear} or use {@link #withIdentity(Identity, Runnable)}.
 *
 * <p>{@link #get()} returns the Identity as-is — it does <em>not</em> check
 * {@code Provenance.tokenExpiry()}. Expiry enforcement is the job of
 * {@code RegulusIdentityExpiryGuard}.
 */
public final class IdentityHolder {

    private static final ThreadLocal<Identity> CURRENT = new ThreadLocal<>();

    private IdentityHolder() {}

    public static void set(Identity identity) {
        CURRENT.set(identity);
    }

    public static Optional<Identity> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Identity require() {
        Identity id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException(
                    "No Regulus Identity bound to the current thread. "
                            + "An IdentityAdapter must populate IdentityHolder before policy-guarded code runs.");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static void withIdentity(Identity identity, Runnable block) {
        Identity previous = CURRENT.get();
        try {
            CURRENT.set(identity);
            block.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
