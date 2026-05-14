package com.regulus.platform.governance.framework;

import com.regulus.platform.governance.FrameworkBinding;
import com.regulus.platform.governance.FrameworkControl;
import com.regulus.platform.governance.FrameworkKind;
import com.regulus.platform.governance.GovernanceFramework;

import java.util.Set;

/**
 * NIST AI Agent Interoperability Profile — PLANNED, Q4 2026.
 *
 * <p>NIST published a concept note on 7 April 2026 outlining voluntary
 * guidelines for AI agents covering identity and authorisation, security
 * and risk management, and monitoring and logging. The final profile is
 * targeted for Q4 2026.
 *
 * <p>This class stubs the categories named in the concept note so adopters
 * can pre-bind Regulus mechanisms. <strong>Identifier strings are
 * provisional</strong> and will be remapped once NIST publishes the final
 * profile.
 */
public final class NistAiRmfAgentInteropProfile implements GovernanceFramework {

    @Override public String id()           { return "nist-ai-rmf-agent-interop"; }
    @Override public String displayName()  { return "NIST AI RMF Agent Interoperability Profile (planned Q4 2026)"; }
    @Override public String version()      { return "Concept note (April 2026) — final IDs TBD"; }
    @Override public FrameworkKind kind()  { return FrameworkKind.VOLUNTARY; }
    @Override public String authorityUrl() { return "https://www.nist.gov/itl/ai-risk-management-framework"; }

    @Override public Set<FrameworkControl> controls() {
        return Set.of(
                new FrameworkControl("AGENT-IDENTITY-1", "Identity and authorisation",
                        "Identity of acting agent",
                        "Each agent has a stable, verifiable identity carried on outbound A2A calls."),
                new FrameworkControl("AGENT-IDENTITY-2", "Identity and authorisation",
                        "Authorisation scoping",
                        "Each agent action is authorised against a defined scope; impersonation is detectable."),
                new FrameworkControl("AGENT-SECURITY-1", "Security and risk management",
                        "Tool-call risk gating",
                        "Tool calls — especially code executors — are gated by risk tier or human confirmation."),
                new FrameworkControl("AGENT-SECURITY-2", "Security and risk management",
                        "Kill switch / oversight",
                        "Operators can halt an agent's execution rapidly under documented authority."),
                new FrameworkControl("AGENT-MONITORING-1", "Monitoring and logging",
                        "Per-invocation logging",
                        "Every agent invocation produces a structured, retrievable record."),
                new FrameworkControl("AGENT-MONITORING-2", "Monitoring and logging",
                        "Cross-agent trace continuity",
                        "A2A hops carry correlation identifiers so end-to-end traces survive cross-agent calls.")
        );
    }

    @Override public Set<FrameworkBinding> bindings() {
        return Set.of(
                new FrameworkBinding("audit-trail", "AGENT-MONITORING-1",
                        "RegulusAuditPlugin emits a record on every consequential agent action."),
                new FrameworkBinding("a2a-envelope", "AGENT-MONITORING-2",
                        "regulus-ai-adk-a2a propagates correlation_id across A2A hops."),
                new FrameworkBinding("a2a-envelope", "AGENT-IDENTITY-1",
                        "RegulusRemoteA2AAgent signs outbound calls with the caller agent's identity when configured."),
                new FrameworkBinding("policy-engine", "AGENT-IDENTITY-2",
                        "RegulusPolicyPlugin enforces tool-allowlist and purpose-binding against the caller's authorised scope."),
                new FrameworkBinding("model-risk-tier", "AGENT-SECURITY-1",
                        "RegulusModelRiskPlugin gates code executors (ContainerCodeExecutor, VertexAiCodeExecutor) by tier."),
                new FrameworkBinding("dual-control-kill-switch", "AGENT-SECURITY-2",
                        "RegulusKillSwitchPlugin provides single-control activation + dual-control deactivation.")
        );
    }
}
