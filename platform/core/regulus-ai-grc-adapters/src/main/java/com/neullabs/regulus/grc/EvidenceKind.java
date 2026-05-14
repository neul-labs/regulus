package com.neullabs.regulus.grc;

public enum EvidenceKind {
    /** Routine evidence that a control fired correctly. */
    CONTROL_TEST,
    /** An incident or anomaly with severity tagging. */
    INCIDENT,
    /** A policy enforcement action (block, require-confirmation). */
    POLICY_ENFORCEMENT,
    /** A documented exception or override accepted by an authorised operator. */
    EXCEPTION
}
