package com.regulus.platform.compliance.profile;

import com.regulus.platform.compliance.AuditSchema;
import com.regulus.platform.compliance.ComplianceProfile;
import com.regulus.platform.compliance.ControlBinding;
import com.regulus.platform.compliance.EventCompactionPolicy;
import com.regulus.platform.compliance.Jurisdiction;
import com.regulus.platform.compliance.ResidencyPolicy;

import java.time.Duration;
import java.util.Set;

public final class EuAiActProfile implements ComplianceProfile {

    @Override public String id() { return "eu-ai-act"; }
    @Override public String displayName() { return "EU AI Act"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.EU; }
    @Override public String citation() { return "EU 2024/1689"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("model-risk-tier", "Art. 9",
                        "Risk-management system across the AI system lifecycle — Regulus enforces tenant-approved tiers per model and code executor."),
                new ControlBinding("audit-trail", "Art. 12",
                        "Automatic recording of events ('logs') sufficient for traceability of the AI system's functioning."),
                new ControlBinding("transparency-disclosure", "Art. 13",
                        "Instructions for use and transparency for downstream deployers — Regulus emits per-invocation provenance records."),
                new ControlBinding("dual-control-kill-switch", "Art. 14",
                        "Human oversight measures: oversight by natural persons including the ability to intervene or interrupt — maps to ADK ToolConfirmation and Regulus dual-control."),
                new ControlBinding("accuracy-robustness-cybersecurity", "Art. 15",
                        "Appropriate level of accuracy, robustness and cybersecurity throughout the AI system lifecycle."),
                new ControlBinding("post-market-monitoring", "Art. 16",
                        "Provider obligations including post-market monitoring system — Regulus audit trail feeds this."),
                new ControlBinding("deployer-obligations", "Art. 26",
                        "Deployer obligations: monitor operation, keep logs, ensure human oversight."),
                new ControlBinding("annex-iii-classification", "Annex III",
                        "High-risk AI systems classification — Regulus model registry tags each model with applicable Annex III categories.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        // AI Act Art. 19: logs kept for 6 months minimum; deployers in regulated sectors often longer.
        return new EventCompactionPolicy(Duration.ofDays(180), Duration.ofDays(365 * 5), true);
    }

    @Override public ResidencyPolicy residency() {
        // No strict residency requirement in the AI Act itself, but pairs with GDPR which does enforce it.
        return new ResidencyPolicy(Set.of(), false, ResidencyPolicy.CrossBorderTransfer.ALLOWED_WITH_SCC);
    }

    @Override public AuditSchema auditSchema() {
        return new AuditSchema(
                Set.of("event_id", "occurred_at", "actor", "action", "result",
                        "model_id", "model_version", "ai_act_risk_tier", "human_oversight_status"),
                AuditSchema.Immutability.MONOTONIC,
                false);
    }
}
