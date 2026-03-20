package com.regulus.platform.compliance.profile;

import com.regulus.platform.compliance.AuditSchema;
import com.regulus.platform.compliance.ComplianceProfile;
import com.regulus.platform.compliance.ControlBinding;
import com.regulus.platform.compliance.EventCompactionPolicy;
import com.regulus.platform.compliance.Jurisdiction;
import com.regulus.platform.compliance.ResidencyPolicy;

import java.time.Duration;
import java.util.Set;

public final class FcaSyscProfile implements ComplianceProfile {

    @Override public String id() { return "fca-sysc"; }
    @Override public String displayName() { return "FCA SYSC + Consumer Duty"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.UK; }
    @Override public String citation() { return "FCA Handbook SYSC; FG22/5; PS22/9"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("senior-management-arrangements", "SYSC 4",
                        "Senior management responsibility for AI decisions — Regulus audit attribution carries actor identity to the SMF holder."),
                new ControlBinding("outsourcing-controls", "SYSC 13",
                        "Outsourcing risk — Regulus model registry treats each LLM provider as an outsource."),
                new ControlBinding("records-retention", "SYSC 9",
                        "Adequate records retained for 5 years (sometimes 7) — Regulus retention compactor."),
                new ControlBinding("consumer-duty-good-outcomes", "FG22/5",
                        "Consumer Duty — Regulus policy guards block agent actions that fail the 4 outcomes (products, price, understanding, support)."),
                new ControlBinding("vulnerable-customer-handling", "FG22/5 §4",
                        "Vulnerable customer protections — Regulus policy guards flag protected categories and require enhanced HITL.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        // SYSC 9: 5 years for most records; MiFID transactions 7 years.
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
                Set.of("event_id", "occurred_at", "actor", "smf_holder", "action", "result",
                        "consumer_duty_outcome", "vulnerable_customer_flag", "fca_lei"),
                AuditSchema.Immutability.SIGNED,
                true);
    }
}
