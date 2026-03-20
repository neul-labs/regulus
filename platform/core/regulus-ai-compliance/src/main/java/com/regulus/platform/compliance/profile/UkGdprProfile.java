package com.regulus.platform.compliance.profile;

import com.regulus.platform.compliance.AuditSchema;
import com.regulus.platform.compliance.ComplianceProfile;
import com.regulus.platform.compliance.ControlBinding;
import com.regulus.platform.compliance.EventCompactionPolicy;
import com.regulus.platform.compliance.Jurisdiction;
import com.regulus.platform.compliance.ResidencyPolicy;

import java.time.Duration;
import java.util.Set;

public final class UkGdprProfile implements ComplianceProfile {

    @Override public String id() { return "uk-gdpr"; }
    @Override public String displayName() { return "UK GDPR + DPA 2018"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.UK; }
    @Override public String citation() { return "UK GDPR; Data Protection Act 2018"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("purpose-binding", "UK GDPR Art. 5(1)(b)",
                        "Purpose limitation — same shape as EU GDPR, ICO enforcement."),
                new ControlBinding("storage-limitation", "UK GDPR Art. 5(1)(e)",
                        "Storage limitation — Regulus retention compactor."),
                new ControlBinding("automated-decisions-safeguards", "UK GDPR Art. 22",
                        "Right not to be subject to automated decisions — ADK ToolConfirmation HITL."),
                new ControlBinding("privacy-by-design", "UK GDPR Art. 25",
                        "Data protection by design — Regulus PII redaction."),
                new ControlBinding("records-of-processing", "UK GDPR Art. 30",
                        "ROPA — Regulus audit trail."),
                new ControlBinding("cross-border-residency", "UK GDPR Arts. 44-49",
                        "Transfers — ICO IDTA, UK Addendum to EU SCCs."),
                new ControlBinding("ico-incident-notification", "UK GDPR Art. 33",
                        "72-hour breach notification to the ICO — Regulus audit pipeline feeds the incident schema.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        return new EventCompactionPolicy(Duration.ofDays(365), Duration.ofDays(365 * 2), true);
    }

    @Override public ResidencyPolicy residency() {
        // UK regions (GCP london) + EU regions via adequacy decision both ways.
        return new ResidencyPolicy(
                Set.of("europe-west2"),
                false,
                ResidencyPolicy.CrossBorderTransfer.ALLOWED_WITH_ADEQUACY_DECISION);
    }

    @Override public AuditSchema auditSchema() {
        return new AuditSchema(
                Set.of("event_id", "occurred_at", "actor", "action", "result",
                        "subject_id", "purpose_code", "lawful_basis", "data_categories"),
                AuditSchema.Immutability.MONOTONIC,
                true);
    }
}
