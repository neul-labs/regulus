package com.neullabs.regulus.adk.a2a.signing;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * Protocol-agnostic shape of an outbound A2A request prior to signing. Holds
 * what RFC 9421 calls the "signature base inputs": method, target URI, the
 * subset of headers the signer commits to, and the request body bytes (used
 * to derive {@code content-digest}). Treat as immutable.
 */
public record A2AEnvelope(
        String method,
        URI targetUri,
        Map<String, String> headers,
        byte[] body) {

    public A2AEnvelope {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(targetUri, "targetUri");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
