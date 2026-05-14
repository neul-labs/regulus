package com.neullabs.regulus.llm.cost;

import com.neullabs.regulus.llm.LlmResponse.CostEstimate;
import com.neullabs.regulus.llm.LlmResponse.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Calculates costs for LLM API calls based on provider pricing.
 * Prices are in USD per 1M tokens (as of late 2024).
 */
public class CostCalculator {

    private static final Logger log = LoggerFactory.getLogger(CostCalculator.class);

    // Pricing per 1M tokens (input, output) in USD
    private static final Map<String, double[]> PRICING = Map.ofEntries(
        // OpenAI
        Map.entry("gpt-4o", new double[]{2.50, 10.00}),
        Map.entry("gpt-4o-mini", new double[]{0.15, 0.60}),
        Map.entry("gpt-4-turbo", new double[]{10.00, 30.00}),
        Map.entry("gpt-4", new double[]{30.00, 60.00}),
        Map.entry("gpt-3.5-turbo", new double[]{0.50, 1.50}),

        // Anthropic
        Map.entry("claude-3-5-sonnet", new double[]{3.00, 15.00}),
        Map.entry("claude-3-opus", new double[]{15.00, 75.00}),
        Map.entry("claude-3-sonnet", new double[]{3.00, 15.00}),
        Map.entry("claude-3-haiku", new double[]{0.25, 1.25}),

        // Google Gemini (Vertex AI)
        Map.entry("gemini-1.5-pro", new double[]{1.25, 5.00}),
        Map.entry("gemini-1.5-flash", new double[]{0.075, 0.30}),
        Map.entry("gemini-1.0-pro", new double[]{0.50, 1.50}),

        // Azure OpenAI (same as OpenAI)
        Map.entry("azure-gpt-4o", new double[]{2.50, 10.00}),
        Map.entry("azure-gpt-4", new double[]{30.00, 60.00})
    );

    private final TokenCounter tokenCounter;

    public CostCalculator() {
        this.tokenCounter = new TokenCounter();
    }

    public CostCalculator(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }

    /**
     * Calculate cost for a completed request.
     *
     * @param model the model used
     * @param usage token usage from the response
     * @return cost estimate in USD
     */
    public CostEstimate calculate(String model, TokenUsage usage) {
        double[] pricing = getPricing(model);
        double inputCost = (usage.promptTokens() / 1_000_000.0) * pricing[0];
        double outputCost = (usage.completionTokens() / 1_000_000.0) * pricing[1];

        return CostEstimate.usd(inputCost, outputCost);
    }

    /**
     * Estimate cost before making a request.
     *
     * @param model the model to use
     * @param prompt the input prompt
     * @param estimatedOutputTokens expected output tokens
     * @return estimated cost in USD
     */
    public CostEstimate estimate(String model, String prompt, int estimatedOutputTokens) {
        int inputTokens = tokenCounter.countTokens(prompt, model);
        double[] pricing = getPricing(model);

        double inputCost = (inputTokens / 1_000_000.0) * pricing[0];
        double outputCost = (estimatedOutputTokens / 1_000_000.0) * pricing[1];

        return CostEstimate.usd(inputCost, outputCost);
    }

    /**
     * Get input price per 1M tokens for a model.
     */
    public double getInputPricePerMillion(String model) {
        return getPricing(model)[0];
    }

    /**
     * Get output price per 1M tokens for a model.
     */
    public double getOutputPricePerMillion(String model) {
        return getPricing(model)[1];
    }

    private double[] getPricing(String model) {
        if (model == null) {
            return new double[]{0.0, 0.0};
        }

        String normalized = normalizeModel(model);
        double[] pricing = PRICING.get(normalized);

        if (pricing == null) {
            log.warn("Unknown model pricing for '{}', using default", model);
            return new double[]{5.0, 15.0}; // Conservative default
        }

        return pricing;
    }

    private String normalizeModel(String model) {
        String lower = model.toLowerCase().trim();

        // OpenAI models
        if (lower.contains("gpt-4o-mini")) return "gpt-4o-mini";
        if (lower.contains("gpt-4o")) return "gpt-4o";
        if (lower.contains("gpt-4-turbo")) return "gpt-4-turbo";
        if (lower.contains("gpt-4")) return "gpt-4";
        if (lower.contains("gpt-3.5")) return "gpt-3.5-turbo";

        // Anthropic models
        if (lower.contains("claude-3-5-sonnet") || lower.contains("claude-3.5-sonnet"))
            return "claude-3-5-sonnet";
        if (lower.contains("claude-3-opus")) return "claude-3-opus";
        if (lower.contains("claude-3-sonnet")) return "claude-3-sonnet";
        if (lower.contains("claude-3-haiku")) return "claude-3-haiku";

        // Gemini models
        if (lower.contains("gemini-1.5-pro") || lower.contains("gemini-pro"))
            return "gemini-1.5-pro";
        if (lower.contains("gemini-1.5-flash") || lower.contains("gemini-flash"))
            return "gemini-1.5-flash";
        if (lower.contains("gemini-1.0-pro")) return "gemini-1.0-pro";

        // Azure prefix
        if (lower.startsWith("azure-")) {
            return "azure-" + normalizeModel(lower.substring(6));
        }

        return model; // Return as-is if not recognized
    }
}
