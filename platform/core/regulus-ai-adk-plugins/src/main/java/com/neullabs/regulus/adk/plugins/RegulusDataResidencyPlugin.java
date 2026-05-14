package com.neullabs.regulus.adk.plugins;

import com.google.adk.plugins.BasePlugin;
import com.neullabs.regulus.compliance.ResidencyPolicy;

import java.util.Set;

/**
 * Validates that the wired {@code SessionService}, {@code ArtifactService},
 * {@code MemoryService}, and model endpoints are pinned to a region on the
 * tenant's allowlist. Fail-closed: if validation fails, the ADK {@code App}
 * refuses to activate.
 *
 * <p>Hooks used:
 * <ul>
 *   <li>{@code BeforeAgentCallback} — last-mile per-call validation (defends
 *       against runtime drift after start-up).</li>
 *   <li>Startup hook (called from {@code App} initialisation by the Regulus
 *       starter) — inspects the wired services' configured regions and refuses
 *       to start the application if any sit outside the allowlist.</li>
 * </ul>
 *
 * <p>Maps to: GDPR / UK GDPR Arts. 44-49 (cross-border transfers), FCA SYSC 13
 * (operational risk including data localisation), PRA SS2/21 §6, NHS DSPT
 * (UK-only for confidential patient information).
 */
public final class RegulusDataResidencyPlugin extends BasePlugin {

    private final ResidencyPolicy policy;

    private RegulusDataResidencyPlugin(ResidencyPolicy policy) {
        super("regulus-data-residency");
        this.policy = policy;
    }

    public static RegulusDataResidencyPlugin allow(String... regions) {
        return new RegulusDataResidencyPlugin(new ResidencyPolicy(
                Set.of(regions),
                false,
                ResidencyPolicy.CrossBorderTransfer.ALLOWED_WITH_SCC));
    }

    public static RegulusDataResidencyPlugin fromPolicy(ResidencyPolicy policy) {
        return new RegulusDataResidencyPlugin(policy);
    }

    public ResidencyPolicy policy() { return policy; }
}
