package com.neullabs.regulus.compliance;

/**
 * Binds a Regulus mechanism to a specific clause of a regulation.
 *
 * @param mechanism stable identifier of the Regulus control, e.g.
 *                  {@code "pii-redaction"}, {@code "dual-control-kill-switch"},
 *                  {@code "data-residency"}, {@code "audit-trail"},
 *                  {@code "model-risk-tier"}.
 * @param clause    citation, e.g. {@code "Art. 12"}, {@code "Annex III(5)(b)"},
 *                  {@code "SYSC 13.9"}.
 * @param rationale plain-English note explaining how the mechanism satisfies
 *                  the clause. Surfaced in the coverage matrix and in audit
 *                  evidence packs.
 */
public record ControlBinding(
        String mechanism,
        String clause,
        String rationale) {
}
