package com.regulus.platform.compliance;

import java.util.Set;

/**
 * Allowlist of cloud regions on which an agent's session / memory / artifact
 * services and model endpoints may run.
 *
 * <p>Region strings use the provider's native naming, e.g. {@code "europe-west2"}
 * for GCP London. {@code RegulusDataResidencyPlugin} validates wired services
 * against this allowlist at startup and refuses {@code App} activation if any
 * service is outside it (fail-closed).
 *
 * @param allowedRegions       set of acceptable region identifiers.
 * @param requireCmek          true if customer-managed encryption keys are mandatory.
 * @param crossBorderTransfer  policy describing whether transfer outside
 *                             {@code allowedRegions} is permissible (e.g. with
 *                             SCCs under GDPR Art. 46).
 */
public record ResidencyPolicy(
        Set<String> allowedRegions,
        boolean requireCmek,
        CrossBorderTransfer crossBorderTransfer) {

    public enum CrossBorderTransfer {
        FORBIDDEN,
        ALLOWED_WITH_SCC,
        ALLOWED_WITH_ADEQUACY_DECISION
    }

    public static ResidencyPolicy unconstrained() {
        return new ResidencyPolicy(Set.of(), false, CrossBorderTransfer.ALLOWED_WITH_SCC);
    }
}
