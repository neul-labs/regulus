package com.neullabs.regulus.adk.a2a;

import com.neullabs.regulus.adk.a2a.signing.A2ARequestSigner;
import com.neullabs.regulus.adk.plugins.AuditSink;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Wraps ADK's {@code com.google.adk.a2a.RemoteA2AAgent} to apply the Regulus
 * envelope on outbound A2A calls: policy + privacy on the request, audit emit
 * on both sides of the round trip, and — when an {@link A2ARequestSigner} is
 * supplied — RFC 9421 HTTP Message Signing for cross-org deployments where
 * audit linking is required.
 */
public final class RegulusRemoteA2AAgent {

    private final URI remoteEndpoint;
    private final AuditSink auditSink;
    private final Optional<A2ARequestSigner> signer;

    public RegulusRemoteA2AAgent(URI remoteEndpoint, AuditSink auditSink, A2ARequestSigner signer) {
        this.remoteEndpoint = Objects.requireNonNull(remoteEndpoint, "remoteEndpoint");
        this.auditSink = auditSink;
        this.signer = Optional.ofNullable(signer);
    }

    public RegulusRemoteA2AAgent(URI remoteEndpoint, AuditSink auditSink) {
        this(remoteEndpoint, auditSink, null);
    }

    public URI remoteEndpoint() { return remoteEndpoint; }

    public AuditSink auditSink() { return auditSink; }

    public Optional<A2ARequestSigner> signer() { return signer; }

    /** Convenience for callers checking whether signing is enabled. */
    public boolean signRequests() { return signer.isPresent(); }
}
