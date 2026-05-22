package com.neullabs.regulus.identity.spi;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Optional;

/**
 * Protocol-agnostic view of an inbound request handed to an
 * {@link IdentityAdapter}. The {@code raw} map carries adapter-specific
 * unboxed objects (e.g. {@code JwtAuthenticationToken} keyed as
 * {@code "springAuth"}) so adapters can dodge re-parsing the wire format.
 */
public record RequestContext(
        String scheme,
        Map<String, String> headers,
        Optional<X509Certificate[]> clientCerts,
        Map<String, Object> raw) {

    public RequestContext {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        clientCerts = clientCerts == null ? Optional.empty() : clientCerts;
        raw = raw == null ? Map.of() : Map.copyOf(raw);
    }
}
