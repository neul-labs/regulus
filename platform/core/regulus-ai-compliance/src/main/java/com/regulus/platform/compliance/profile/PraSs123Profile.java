package com.regulus.platform.compliance.profile;

import com.regulus.platform.compliance.AuditSchema;
import com.regulus.platform.compliance.ComplianceProfile;
import com.regulus.platform.compliance.ControlBinding;
import com.regulus.platform.compliance.EventCompactionPolicy;
import com.regulus.platform.compliance.Jurisdiction;
import com.regulus.platform.compliance.ResidencyPolicy;

import java.time.Duration;
import java.util.Set;

public final class PraSs123Profile implements ComplianceProfile {

    @Override public String id() { return "pra-ss1-23"; }
    @Override public String displayName() { return "PRA SS1/23 — Model Risk Management"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.UK; }
    @Override public String citation() { return "PRA Supervisory Statement 1/23"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("model-inventory", "SS1/23 §2",
                        "Comprehensive inventory of models — Regulus model registry maintains the canonical list."),
                new ControlBinding("model-risk-tier", "SS1/23 §3",
                        "Model risk tiering based on materiality — Regulus tiers models and blocks tier-exceeding invocations."),
                new ControlBinding("model-validation", "SS1/23 §4",
                        "Independent model validation — Regulus persists validation evidence in the audit trail."),
                new ControlBinding("model-monitoring", "SS1/23 §5",
                        "Ongoing monitoring — Regulus observability emits drift/quality metrics per model."),
                new ControlBinding("kill-switch-readiness", "SS1/23 §6",
                        "Capability to switch off a model rapidly — Regulus dual-control kill switch.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        return new EventCompactionPolicy(Duration.ofDays(365 * 5), Duration.ofDays(365 * 7), false);
    }

    @Override public ResidencyPolicy residency() {
        return new ResidencyPolicy(
                Set.of("europe-west2"),
                true,
                ResidencyPolicy.CrossBorderTransfer.ALLOWED_WITH_ADEQUACY_DECISION);
    }

    @Override public AuditSchema auditSchema() {
        return new AuditSchema(
                Set.of("event_id", "occurred_at", "actor", "action", "result",
                        "model_id", "model_version", "model_risk_tier", "validation_status"),
                AuditSchema.Immutability.SIGNED,
                false);
    }
}
