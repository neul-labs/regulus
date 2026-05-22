package com.neullabs.regulus.identity.spi;

import com.neullabs.regulus.identity.Identity;

/**
 * Mints a Regulus {@link Identity} from a protocol-specific authenticated
 * request. Adapters MUST be:
 *
 * <ul>
 *   <li><b>Idempotent</b> — repeated calls with the same {@link RequestContext}
 *       produce identical Identities.</li>
 *   <li><b>Fail-closed</b> — any verification failure throws
 *       {@link AuthenticationException}; never return a half-populated
 *       Identity.</li>
 *   <li><b>Jurisdiction-populating</b> — the resulting
 *       {@code Claims.jurisdiction()} must be set; downstream residency and
 *       compliance evaluation depends on it.</li>
 * </ul>
 */
public interface IdentityAdapter {

    /**
     * Stable identifier for this adapter (e.g. {@code "oidc"}, {@code "saml"},
     * {@code "mtls"}). Written into {@code Identity.Provenance.adapterId} so
     * audit records can tell which protocol vouched for the caller.
     */
    String adapterId();

    Identity authenticate(RequestContext context) throws AuthenticationException;

    /**
     * Lower values run first when multiple adapters are registered. The
     * default of 100 leaves room for stronger schemes (e.g. mTLS at 50) to
     * pre-empt weaker ones (e.g. bearer-token OIDC at 100).
     */
    default int order() {
        return 100;
    }
}
