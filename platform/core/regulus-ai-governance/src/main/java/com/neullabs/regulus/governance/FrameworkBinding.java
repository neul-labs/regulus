package com.neullabs.regulus.governance;

/**
 * Binds a Regulus mechanism (the same identifier used by
 * {@link com.neullabs.regulus.compliance.ControlBinding}) to a framework
 * control id.
 *
 * @param mechanism stable Regulus control name, e.g. {@code "pii-redaction"},
 *                  {@code "dual-control-kill-switch"}, {@code "audit-trail"}.
 * @param controlId the framework's identifier, e.g. {@code "GOVERN-1.1"},
 *                  {@code "A.6.2.2"}.
 * @param rationale plain-English note explaining how the mechanism satisfies
 *                  the control. Renders in the coverage matrix.
 */
public record FrameworkBinding(
        String mechanism,
        String controlId,
        String rationale) {
}
