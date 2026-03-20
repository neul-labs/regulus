package com.regulus.platform.llm;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provider-agnostic LLM client interface.
 * Supports multiple providers (Gemini, OpenAI, Anthropic, Azure) with unified API.
 */
public interface LlmClient {

    /**
     * Generate a response from the LLM.
     *
     * @param request the LLM request containing prompt, parameters, etc.
     * @return the LLM response with content, token usage, and cost
     */
    LlmResponse generate(LlmRequest request);

    /**
     * Generate a response asynchronously.
     *
     * @param request the LLM request
     * @return a future containing the LLM response
     */
    CompletableFuture<LlmResponse> generateAsync(LlmRequest request);

    /**
     * Stream a response from the LLM.
     *
     * @param request the LLM request
     * @param handler callback for each streamed token
     * @return the final complete response
     */
    LlmResponse generateStreaming(LlmRequest request, StreamingHandler handler);

    /**
     * Get the provider name (e.g., "openai", "anthropic", "gemini").
     */
    String getProviderName();

    /**
     * Get the model name being used.
     */
    String getModelName();

    /**
     * Check if the client is available and configured.
     */
    boolean isAvailable();

    /**
     * Get the supported capabilities of this provider.
     */
    List<LlmCapability> getCapabilities();

    /**
     * Handler for streaming responses.
     */
    interface StreamingHandler {
        /**
         * Called for each token/chunk received.
         */
        void onToken(String token);

        /**
         * Called when streaming completes successfully.
         */
        default void onComplete(LlmResponse finalResponse) {}

        /**
         * Called when an error occurs during streaming.
         */
        default void onError(Throwable error) {}

        /**
         * Called with metadata updates during streaming (e.g., token counts).
         */
        default void onMetadata(StreamingMetadata metadata) {}
    }

    /**
     * Metadata provided during streaming.
     */
    record StreamingMetadata(
        int tokensStreamed,
        long elapsedMs,
        String provider,
        String model
    ) {}

    /**
     * LLM capabilities.
     */
    enum LlmCapability {
        TEXT_GENERATION,
        CHAT,
        FUNCTION_CALLING,
        VISION,
        STREAMING,
        EMBEDDINGS
    }
}
