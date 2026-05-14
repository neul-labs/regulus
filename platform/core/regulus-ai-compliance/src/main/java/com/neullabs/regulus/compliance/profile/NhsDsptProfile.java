package com.neullabs.regulus.compliance.profile;

import com.neullabs.regulus.compliance.AuditSchema;
import com.neullabs.regulus.compliance.ComplianceProfile;
import com.neullabs.regulus.compliance.ControlBinding;
import com.neullabs.regulus.compliance.EventCompactionPolicy;
import com.neullabs.regulus.compliance.Jurisdiction;
import com.neullabs.regulus.compliance.ResidencyPolicy;

import java.time.Duration;
import java.util.Set;

public final class NhsDsptProfile implements ComplianceProfile {

    @Override public String id() { return "nhs-dspt"; }
    @Override public String displayName() { return "NHS Data Security & Protection Toolkit"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.UK; }
    @Override public String citation() { return "NHS DSPT (current edition)"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("personal-data-protection", "DSPT 1.x",
                        "Personal data is protected — Regulus PII redaction on NHS Number, demographic codes, and clinical free-text patterns."),
                new ControlBinding("staff-responsibility", "DSPT 4.x",
                        "Staff are aware of their responsibilities — Regulus audit attribution surfaces clinician identity on every AI-assisted decision."),
                new ControlBinding("incident-management", "DSPT 6.x",
                        "Incidents are managed — Regulus audit pipeline feeds the IG SIRI process."),
                new ControlBinding("access-control", "DSPT 8.x",
                        "Access is controlled and auditable — Regulus policy guards enforce role-based access on agent tools.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        // Records of Processing under NHS records management code: typically 8 years for adult records, longer for paediatric.
        return new EventCompactionPolicy(Duration.ofDays(365 * 8), Duration.ofDays(365 * 25), false);
    }

    @Override public ResidencyPolicy residency() {
        return new ResidencyPolicy(
                Set.of("europe-west2"),
                true,
                ResidencyPolicy.CrossBorderTransfer.FORBIDDEN);
    }

    @Override public AuditSchema auditSchema() {
        return new AuditSchema(
                Set.of("event_id", "occurred_at", "actor", "clinician_smartcard_id", "action", "result",
                        "nhs_number_hashed", "care_setting", "lawful_basis_health"),
                AuditSchema.Immutability.SIGNED,
                true);
    }
}
