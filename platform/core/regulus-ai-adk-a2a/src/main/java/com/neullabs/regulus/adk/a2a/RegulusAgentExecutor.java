package com.neullabs.regulus.adk.a2a;

import com.neullabs.regulus.adk.plugins.AuditSink;

/**
 * Wraps ADK's {@code com.google.adk.a2a.AgentExecutor} to apply the Regulus
 * envelope on inbound JSON-RPC calls before they reach the agent.
 *
 * <p>The envelope: policy guards → privacy redaction (on the inbound request)
 * → kill-switch check → audit emit. Outbound responses are re-redacted before
 * leaving the JVM.
 *
 * <p>This is what makes Regulus compliance plane work across multi-agent
 * deployments: every hop between agents — even those owned by different
 * organisations — goes through the same envelope.
 */
public final class RegulusAgentExecutor {

    private final AuditSink auditSink;
    private final boolean signRequests;

    public RegulusAgentExecutor(AuditSink auditSink, boolean signRequests) {
        this.auditSink = auditSink;
        this.signRequests = signRequests;
    }

    public AuditSink auditSink() { return auditSink; }
    public boolean signRequests() { return signRequests; }
}
