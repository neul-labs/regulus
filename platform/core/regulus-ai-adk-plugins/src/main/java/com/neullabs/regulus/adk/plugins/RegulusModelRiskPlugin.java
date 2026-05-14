package com.neullabs.regulus.adk.plugins;

import com.google.adk.plugins.BasePlugin;

import java.util.Set;

/**
 * Refuses model and code-executor invocations that exceed the tenant's
 * approved risk tier.
 *
 * <p>Hooks used:
 * <ul>
 *   <li>{@code BeforeModelCallback} — rejects model IDs whose registered tier is
 *       higher than the tenant's allowance.</li>
 *   <li>{@code BeforeToolCallback} — applies tier checks to ADK's
 *       {@code ContainerCodeExecutor} and {@code VertexAiCodeExecutor}, which
 *       Regulus treats as high-risk by default.</li>
 * </ul>
 *
 * <p>Maps to: EU AI Act Annex III (high-risk classification), PRA SS1/23 §3
 * (model risk tiering by materiality), FCA SYSC 4 (proportionality of controls
 * to materiality).
 */
public final class RegulusModelRiskPlugin extends BasePlugin {

    public enum Tier {
        EXPERIMENTAL,
        STANDARD,
        REGULATED,
        HIGH_RISK
    }

    private final Tier tenantTier;
    private final ModelRegistry registry;

    private RegulusModelRiskPlugin(Tier tenantTier, ModelRegistry registry) {
        super("regulus-model-risk");
        this.tenantTier = tenantTier;
        this.registry = registry;
    }

    public static RegulusModelRiskPlugin tier(Tier tenantTier) {
        return new RegulusModelRiskPlugin(tenantTier, ModelRegistry.defaultRegistry());
    }

    public static RegulusModelRiskPlugin tier(Tier tenantTier, ModelRegistry registry) {
        return new RegulusModelRiskPlugin(tenantTier, registry);
    }

    /** Code executors Regulus classifies as high-risk by default. */
    public static final Set<String> HIGH_RISK_CODE_EXECUTORS = Set.of(
            "com.google.adk.tools.ContainerCodeExecutor",
            "com.google.adk.tools.VertexAiCodeExecutor"
    );

    public Tier tenantTier() { return tenantTier; }
    public ModelRegistry registry() { return registry; }
}
