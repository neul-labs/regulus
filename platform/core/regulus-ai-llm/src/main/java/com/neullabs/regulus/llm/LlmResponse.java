package com.neullabs.regulus.llm;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response from an LLM provider.
 */
public record LlmResponse(
    String id,
    String requestId,
    String content,
    List<ToolCall> toolCalls,
    TokenUsage tokenUsage,
    CostEstimate cost,
    String provider,
    String model,
    FinishReason finishReason,
    Duration latency,
    Instant timestamp,
    Map<String, Object> metadata,
    boolean success,
    String errorMessage
) {
    public static LlmResponse success(
            String requestId,
            String content,
            TokenUsage tokenUsage,
            String provider,
            String model,
            Duration latency) {
        return new LlmResponse(
            java.util.UUID.randomUUID().toString(),
            requestId,
            content,
            List.of(),
            tokenUsage,
            null,
            provider,
            model,
            FinishReason.STOP,
            latency,
            Instant.now(),
            Map.of(),
            true,
            null
        );
    }

    public static LlmResponse withToolCalls(
            String requestId,
            List<ToolCall> toolCalls,
            TokenUsage tokenUsage,
            String provider,
            String model,
            Duration latency) {
        return new LlmResponse(
            java.util.UUID.randomUUID().toString(),
            requestId,
            null,
            toolCalls,
            tokenUsage,
            null,
            provider,
            model,
            FinishReason.TOOL_CALLS,
            latency,
            Instant.now(),
            Map.of(),
            true,
            null
        );
    }

    public static LlmResponse error(String requestId, String provider, String errorMessage) {
        return new LlmResponse(
            java.util.UUID.randomUUID().toString(),
            requestId,
            null,
            List.of(),
            TokenUsage.zero(),
            null,
            provider,
            null,
            FinishReason.ERROR,
            Duration.ZERO,
            Instant.now(),
            Map.of(),
            false,
            errorMessage
        );
    }

    public static LlmResponse unavailable(String provider, String reason) {
        return error(null, provider, "Provider unavailable: " + reason);
    }

    /**
     * Tool call requested by the LLM.
     */
    public record ToolCall(
        String id,
        String name,
        Map<String, Object> arguments
    ) {}

    /**
     * Token usage statistics.
     */
    public record TokenUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens
    ) {
        public static TokenUsage zero() {
            return new TokenUsage(0, 0, 0);
        }

        public static TokenUsage of(int prompt, int completion) {
            return new TokenUsage(prompt, completion, prompt + completion);
        }
    }

    /**
     * Cost estimate for the request.
     */
    public record CostEstimate(
        double inputCost,
        double outputCost,
        double totalCost,
        String currency
    ) {
        public static CostEstimate usd(double input, double output) {
            return new CostEstimate(input, output, input + output, "USD");
        }
    }

    /**
     * Reason for completion.
     */
    public enum FinishReason {
        STOP,           // Natural completion
        LENGTH,         // Max tokens reached
        TOOL_CALLS,     // Model wants to call tools
        CONTENT_FILTER, // Content filtered
        ERROR           // Error occurred
    }
}
