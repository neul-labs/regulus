package com.regulus.platform.adk.plugins;

import java.util.Map;

/**
 * Maps model IDs to their assigned risk tier. The default registry ships with
 * conservative tiers for common Gemini / OpenAI / Anthropic models; real
 * deployments override via {@link #of(Map)} or by wiring a persistent store.
 */
public interface ModelRegistry {

    RegulusModelRiskPlugin.Tier tierOf(String modelId);

    static ModelRegistry of(Map<String, RegulusModelRiskPlugin.Tier> mapping) {
        return modelId -> mapping.getOrDefault(modelId, RegulusModelRiskPlugin.Tier.EXPERIMENTAL);
    }

    static ModelRegistry defaultRegistry() {
        return of(Map.ofEntries(
                Map.entry("gemini-2.5-flash", RegulusModelRiskPlugin.Tier.STANDARD),
                Map.entry("gemini-2.5-pro", RegulusModelRiskPlugin.Tier.REGULATED),
                Map.entry("gemini-2.0-flash", RegulusModelRiskPlugin.Tier.STANDARD),
                Map.entry("gpt-4o", RegulusModelRiskPlugin.Tier.REGULATED),
                Map.entry("gpt-4o-mini", RegulusModelRiskPlugin.Tier.STANDARD),
                Map.entry("claude-opus-4-7", RegulusModelRiskPlugin.Tier.REGULATED),
                Map.entry("claude-sonnet-4-6", RegulusModelRiskPlugin.Tier.STANDARD),
                Map.entry("claude-haiku-4-5", RegulusModelRiskPlugin.Tier.STANDARD)
        ));
    }
}
