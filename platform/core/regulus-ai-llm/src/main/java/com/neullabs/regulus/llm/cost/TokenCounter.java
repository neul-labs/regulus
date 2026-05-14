package com.neullabs.regulus.llm.cost;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token counter using tiktoken (via jtokkit) for accurate token estimation.
 * Supports multiple model families with appropriate tokenizers.
 */
public class TokenCounter {

    private static final Logger log = LoggerFactory.getLogger(TokenCounter.class);

    private final EncodingRegistry registry;
    private final Map<String, Encoding> encodingCache = new ConcurrentHashMap<>();

    // Model to encoding mapping
    private static final Map<String, EncodingType> MODEL_ENCODINGS = Map.of(
        "gpt-4", EncodingType.CL100K_BASE,
        "gpt-4o", EncodingType.CL100K_BASE,
        "gpt-4o-mini", EncodingType.CL100K_BASE,
        "gpt-3.5-turbo", EncodingType.CL100K_BASE,
        "claude", EncodingType.CL100K_BASE,  // Approximation
        "gemini", EncodingType.CL100K_BASE   // Approximation
    );

    public TokenCounter() {
        this.registry = Encodings.newDefaultEncodingRegistry();
        log.info("Token counter initialized with tiktoken encodings");
    }

    /**
     * Count tokens in text for a specific model.
     *
     * @param text the text to count tokens for
     * @param model the model name (e.g., "gpt-4", "claude-3-sonnet")
     * @return estimated token count
     */
    public int countTokens(String text, String model) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        Encoding encoding = getEncoding(model);
        return encoding.countTokens(text);
    }

    /**
     * Count tokens in a list of messages (chat format).
     * Accounts for message overhead tokens.
     *
     * @param messages list of message contents
     * @param model the model name
     * @return estimated token count
     */
    public int countChatTokens(java.util.List<String> messages, String model) {
        int total = 0;
        Encoding encoding = getEncoding(model);

        for (String message : messages) {
            // Each message has ~4 tokens of overhead for role, formatting
            total += encoding.countTokens(message) + 4;
        }

        // Additional overhead for chat format
        total += 3;

        return total;
    }

    /**
     * Estimate tokens for a prompt + expected completion.
     *
     * @param prompt the input prompt
     * @param model the model name
     * @param estimatedCompletionLength expected completion length in characters
     * @return estimated total token count
     */
    public TokenEstimate estimate(String prompt, String model, int estimatedCompletionLength) {
        int promptTokens = countTokens(prompt, model);
        // Rough estimate: ~4 characters per token on average
        int estimatedCompletionTokens = estimatedCompletionLength / 4;

        return new TokenEstimate(promptTokens, estimatedCompletionTokens, promptTokens + estimatedCompletionTokens);
    }

    private Encoding getEncoding(String model) {
        return encodingCache.computeIfAbsent(normalizeModel(model), m -> {
            EncodingType type = MODEL_ENCODINGS.getOrDefault(m, EncodingType.CL100K_BASE);
            return registry.getEncoding(type);
        });
    }

    private String normalizeModel(String model) {
        if (model == null) {
            return "gpt-4";
        }

        String lower = model.toLowerCase();

        if (lower.contains("gpt-4")) return "gpt-4";
        if (lower.contains("gpt-3.5")) return "gpt-3.5-turbo";
        if (lower.contains("claude")) return "claude";
        if (lower.contains("gemini")) return "gemini";

        return "gpt-4"; // Default fallback
    }

    /**
     * Token estimate with breakdown.
     */
    public record TokenEstimate(
        int promptTokens,
        int estimatedCompletionTokens,
        int totalTokens
    ) {}
}
