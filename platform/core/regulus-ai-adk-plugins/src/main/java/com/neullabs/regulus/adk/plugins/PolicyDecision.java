package com.neullabs.regulus.adk.plugins;

public sealed interface PolicyDecision {

    record Allow() implements PolicyDecision {}

    /** Block with a structured reason that lands in the audit trail. */
    record Block(String code, String reason, String clauseCitation) implements PolicyDecision {}

    /** Require dual-control confirmation via ADK {@code ToolConfirmation} before proceeding. */
    record RequireConfirmation(String code, String reason) implements PolicyDecision {}

    Allow ALLOW = new Allow();
}
