package com.neullabs.regulus.adk.a2a;

import com.neullabs.regulus.adk.a2a.signing.A2ARequestSigner;
import com.neullabs.regulus.adk.plugins.AuditSink;

import java.util.Optional;

/**
 * Wraps ADK's {@code com.google.adk.a2a.AgentExecutor} to apply the Regulus
 * envelope on inbound JSON-RPC calls before they reach the agent.
 *
 * <p>The envelope: signature verification (if a signer is configured) →
 * Identity binding via {@code IdentityHolder} → policy guards → privacy
 * redaction (on the inbound request) → kill-switch check → audit emit.
 * Outbound responses are re-redacted before leaving the JVM.
 *
 * <p>Signature verification happens in a pre-dispatch filter that lives
 * alongside this executor; the verified caller's
 * {@code com.neullabs.regulus.identity.Identity} is placed into
 * {@code IdentityHolder} <em>before</em> any plugin or executor logic runs.
 */
public final class RegulusAgentExecutor {

    private final AuditSink auditSink;
    private final Optional<A2ARequestSigner> signer;

    public RegulusAgentExecutor(AuditSink auditSink, A2ARequestSigner signer) {
        this.auditSink = auditSink;
        this.signer = Optional.ofNullable(signer);
    }

    public RegulusAgentExecutor(AuditSink auditSink) {
        this(auditSink, null);
    }

    public AuditSink auditSink() { return auditSink; }

    public Optional<A2ARequestSigner> signer() { return signer; }

    public boolean signRequests() { return signer.isPresent(); }
}
