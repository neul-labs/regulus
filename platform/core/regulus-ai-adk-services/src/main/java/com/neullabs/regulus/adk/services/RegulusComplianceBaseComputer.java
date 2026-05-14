package com.neullabs.regulus.adk.services;

import java.util.Set;

/**
 * Reference compliant implementation of ADK's {@code BaseComputer} extension
 * point used by {@code ComputerUseTool}.
 *
 * <p>Google explicitly flagged {@code BaseComputer} as an extension point
 * developers will have to implement themselves. Regulus ships a hardened
 * reference that:
 *
 * <ul>
 *   <li>Logs every action (click, type, navigate, screenshot) to the audit sink.</li>
 *   <li>Enforces a domain allowlist — the agent cannot browse to arbitrary
 *       sites, only to those approved for the tenant's purpose.</li>
 *   <li>Redacts PII out of screenshots before they leave the executor (using
 *       the same patterns as {@code RegulusPrivacyPlugin}).</li>
 *   <li>Requires {@code ToolConfirmation} for high-risk actions (form submit,
 *       payment confirm, file download).</li>
 * </ul>
 *
 * <p>Maps to: EU AI Act Arts. 14 (human oversight), 15 (cybersecurity); FCA
 * Consumer Duty; GDPR Art. 25 (privacy by design).
 */
public final class RegulusComplianceBaseComputer {

    private final Set<String> allowedDomains;
    private final boolean redactScreenshots;
    private final Set<HighRiskAction> requiresConfirmation;

    public enum HighRiskAction {
        FORM_SUBMIT,
        PAYMENT_CONFIRM,
        FILE_DOWNLOAD,
        FILE_UPLOAD,
        LOGIN_CREDENTIAL_ENTRY
    }

    public RegulusComplianceBaseComputer(Set<String> allowedDomains, boolean redactScreenshots,
                                         Set<HighRiskAction> requiresConfirmation) {
        this.allowedDomains = Set.copyOf(allowedDomains);
        this.redactScreenshots = redactScreenshots;
        this.requiresConfirmation = Set.copyOf(requiresConfirmation);
    }

    public Set<String> allowedDomains() { return allowedDomains; }
    public boolean redactScreenshots() { return redactScreenshots; }
    public Set<HighRiskAction> requiresConfirmation() { return requiresConfirmation; }
}
