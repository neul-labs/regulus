package com.neullabs.regulus.adk.plugins;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelRegistryTest {

    @Test
    void defaultRegistryAssignsKnownTiers() {
        ModelRegistry reg = ModelRegistry.defaultRegistry();
        assertThat(reg.tierOf("gemini-2.5-flash")).isEqualTo(RegulusModelRiskPlugin.Tier.STANDARD);
        assertThat(reg.tierOf("gemini-2.5-pro")).isEqualTo(RegulusModelRiskPlugin.Tier.REGULATED);
        assertThat(reg.tierOf("gpt-4o")).isEqualTo(RegulusModelRiskPlugin.Tier.REGULATED);
        assertThat(reg.tierOf("claude-opus-4-7")).isEqualTo(RegulusModelRiskPlugin.Tier.REGULATED);
    }

    @Test
    void unknownModelDefaultsToExperimental() {
        assertThat(ModelRegistry.defaultRegistry().tierOf("custom-fine-tune"))
                .isEqualTo(RegulusModelRiskPlugin.Tier.EXPERIMENTAL);
    }

    @Test
    void customRegistryOverrides() {
        ModelRegistry reg = ModelRegistry.of(Map.of(
                "internal-model-v2", RegulusModelRiskPlugin.Tier.HIGH_RISK));
        assertThat(reg.tierOf("internal-model-v2"))
                .isEqualTo(RegulusModelRiskPlugin.Tier.HIGH_RISK);
        assertThat(reg.tierOf("not-listed")).isEqualTo(RegulusModelRiskPlugin.Tier.EXPERIMENTAL);
    }

    @Test
    void highRiskCodeExecutorsListIncludesGoogleExecutors() {
        assertThat(RegulusModelRiskPlugin.HIGH_RISK_CODE_EXECUTORS)
                .contains("com.google.adk.tools.ContainerCodeExecutor")
                .contains("com.google.adk.tools.VertexAiCodeExecutor");
    }
}
