package com.regulus.platform.governance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Snapshot of an AI-governance program for a tenant: which frameworks are
 * active, what each control's implementation status is, and (optionally) a
 * formal ISO 42001 Statement of Applicability.
 *
 * <p>Feeds the coverage matrix view, the gap-analysis output, and the
 * SoA artefact that ISO 42001 certification requires.
 *
 * @param frameworks       the active governance frameworks for this tenant
 * @param statusByControlId per-control implementation status (controlId → status)
 * @param soa              optional Statement of Applicability (mandatory for
 *                         ISO 42001 certification only)
 */
public record GovernanceProgramState(
        List<GovernanceFramework> frameworks,
        Map<String, ControlImplementationStatus> statusByControlId,
        Optional<StatementOfApplicability> soa) {

    /** Implemented + Partial controls / total in-scope controls. */
    public double implementationRatio() {
        long total = statusByControlId.values().stream()
                .filter(s -> s != ControlImplementationStatus.NOT_APPLICABLE)
                .count();
        if (total == 0) return 0.0;
        long implemented = statusByControlId.values().stream()
                .filter(s -> s == ControlImplementationStatus.IMPLEMENTED
                          || s == ControlImplementationStatus.PARTIAL)
                .count();
        return (double) implemented / total;
    }

    /** Control ids whose status is {@code GAP}. */
    public List<String> gaps() {
        return statusByControlId.entrySet().stream()
                .filter(e -> e.getValue() == ControlImplementationStatus.GAP)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
