package com.regulus.platform.adk.a2a;

import com.regulus.platform.adk.plugins.AuditSink;

import java.net.URI;

/**
 * Wraps ADK's {@code com.google.adk.a2a.RemoteA2AAgent} to apply the Regulus
 * envelope on outbound A2A calls: policy + privacy on the request, audit emit
 * on both sides of the round trip, optional request signing for cross-org
 * deployments where audit linking is required.
 */
public final class RegulusRemoteA2AAgent {

    private final URI remoteEndpoint;
    private final AuditSink auditSink;
    private final boolean signRequests;

    public RegulusRemoteA2AAgent(URI remoteEndpoint, AuditSink auditSink, boolean signRequests) {
        this.remoteEndpoint = remoteEndpoint;
        this.auditSink = auditSink;
        this.signRequests = signRequests;
    }

    public URI remoteEndpoint() { return remoteEndpoint; }
    public AuditSink auditSink() { return auditSink; }
    public boolean signRequests() { return signRequests; }
}
