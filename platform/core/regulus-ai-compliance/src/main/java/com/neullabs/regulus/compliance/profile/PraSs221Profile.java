package com.neullabs.regulus.compliance.profile;

import com.neullabs.regulus.compliance.AuditSchema;
import com.neullabs.regulus.compliance.ComplianceProfile;
import com.neullabs.regulus.compliance.ControlBinding;
import com.neullabs.regulus.compliance.EventCompactionPolicy;
import com.neullabs.regulus.compliance.ResidencyPolicy;
import com.neullabs.regulus.identity.Jurisdiction;

import java.time.Duration;
import java.util.Set;

public final class PraSs221Profile implements ComplianceProfile {

    @Override public String id() { return "pra-ss2-21"; }
    @Override public String displayName() { return "PRA SS2/21 — Outsourcing & Third-Party Risk"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.UK; }
    @Override public String citation() { return "PRA Supervisory Statement 2/21"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("third-party-register", "SS2/21 §3",
                        "Register of outsourcing arrangements — Regulus model registry doubles as the register entry for each LLM provider."),
                new ControlBinding("data-residency", "SS2/21 §6",
                        "Data residency expectations — Regulus residency plugin pins regions and blocks cross-border drift."),
                new ControlBinding("exit-plan", "SS2/21 §10",
                        "Documented exit plan — Regulus kill switch + provider abstraction satisfy the technical exit primitive."),
                new ControlBinding("audit-rights", "SS2/21 §7",
                        "Audit rights over the outsource — Regulus audit log is the artefact the auditor inspects.")
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
                        "third_party_id", "third_party_criticality", "exit_plan_ref"),
                AuditSchema.Immutability.MONOTONIC,
                false);
    }
}
