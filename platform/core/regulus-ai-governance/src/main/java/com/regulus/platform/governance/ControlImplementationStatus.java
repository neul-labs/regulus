package com.regulus.platform.governance;

/**
 * Per-control implementation status used by {@link GovernanceProgramState}
 * and the ISO 42001 {@link StatementOfApplicability}.
 */
public enum ControlImplementationStatus {
    IMPLEMENTED,
    PARTIAL,
    MAPPED_NOT_IMPLEMENTED,
    NOT_APPLICABLE,
    GAP
}
