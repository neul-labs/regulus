package com.regulus.platform.llm.provider;

import com.regulus.platform.llm.LlmClient;
import com.regulus.platform.llm.LlmRequest;
import com.regulus.platform.llm.LlmResponse;
import com.regulus.platform.llm.LlmResponse.TokenUsage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiStreamingChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Google Gemini LLM client implementation using LangChain4j Vertex AI.
 * This provides ADK-compatible LLM support for Regulus agents.
 */
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final long STREAMING_TIMEOUT_SECONDS = 300; // 5 minutes

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final String modelName;
    private final String projectId;
    private final String location;
    private final boolean available;

    /**
     * Create a Gemini client using Vertex AI.
     *
     * @param projectId GCP project ID
     * @param location GCP region (e.g., "europe-west2", "us-central1")
     * @param modelName Gemini model (e.g., "gemini-1.5-pro", "gemini-1.5-flash")
     */
    public GeminiLlmClient(String projectId, String location, String modelName) {
        this.modelName = modelName;
        this.projectId = projectId;
        this.location = location;

        if (projectId != null && !projectId.isBlank()) {
            try {
                this.chatModel = VertexAiGeminiChatModel.builder()
                    .project(projectId)
                    .location(location)
                    .modelName(modelName)
                    .build();

                this.streamingChatModel = VertexAiGeminiStreamingChatModel.builder()
                    .project(projectId)
                    .location(location)
                    .modelName(modelName)
                    .build();

                this.available = true;
                log.info("Gemini client initialized with streaming: project={}, location={}, model={}",
                    projectId, location, modelName);
            } catch (Exception e) {
                log.error("Failed to initialize Gemini client", e);
                throw new IllegalStateException("Gemini initialization failed: " + e.getMessage(), e);
            }
        } else {
            this.chatModel = null;
            this.streamingChatModel = null;
            this.available = false;
            log.warn("Gemini client not available - no project ID provided");
        }
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (!available) {
            return LlmResponse.unavailable(getProviderName(), "GCP project not configured");
        }

        Instant start = Instant.now();
        try {
            List<ChatMessage> messages = convertMessages(request);
            Response<AiMessage> response = chatModel.generate(messages);

            Duration latency = Duration.between(start, Instant.now());
            String content = response.content().text();

            TokenUsage usage = TokenUsage.zero();
            if (response.tokenUsage() != null) {
                usage = TokenUsage.of(
                    response.tokenUsage().inputTokenCount(),
                    response.tokenUsage().outputTokenCount()
                );
            }

            log.debug("Gemini response: {} tokens in {}ms", usage.totalTokens(), latency.toMillis());

            return LlmResponse.success(request.id(), content, usage, getProviderName(), modelName, latency);

        } catch (Exception e) {
            log.error("Gemini request failed", e);
            return LlmResponse.error(request.id(), getProviderName(), e.getMessage());
        }
    }

    @Override
    public CompletableFuture<LlmResponse> generateAsync(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> generate(request));
    }

    @Override
    public LlmResponse generateStreaming(LlmRequest request, StreamingHandler handler) {
        if (!available) {
            LlmResponse error = LlmResponse.unavailable(getProviderName(), "GCP project not configured");
            handler.onError(new IllegalStateException("Gemini not available"));
            return error;
        }

        Instant start = Instant.now();
        List<ChatMessage> messages = convertMessages(request);

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder contentBuilder = new StringBuilder();
        AtomicInteger tokenCount = new AtomicInteger(0);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<Response<AiMessage>> responseRef = new AtomicReference<>();

        try {
            streamingChatModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    if (token != null && !token.isEmpty()) {
                        contentBuilder.append(token);
                        tokenCount.incrementAndGet();
                        handler.onToken(token);

                        // Send metadata periodically
                        if (tokenCount.get() % 10 == 0) {
                            handler.onMetadata(new StreamingMetadata(
                                tokenCount.get(),
                                Duration.between(start, Instant.now()).toMillis(),
                                getProviderName(),
                                modelName
                            ));
                        }
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    responseRef.set(response);
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Gemini streaming error", error);
                    errorRef.set(error);
                    handler.onError(error);
                    latch.countDown();
                }
            });

            // Wait for completion
            boolean completed = latch.await(STREAMING_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!completed) {
                String errorMsg = "Streaming timeout after " + STREAMING_TIMEOUT_SECONDS + " seconds";
                log.error(errorMsg);
                return LlmResponse.error(request.id(), getProviderName(), errorMsg);
            }

            if (errorRef.get() != null) {
                return LlmResponse.error(request.id(), getProviderName(), errorRef.get().getMessage());
            }

            Duration latency = Duration.between(start, Instant.now());
            String content = contentBuilder.toString();

            // Get token usage from response if available
            TokenUsage usage = TokenUsage.zero();
            Response<AiMessage> response = responseRef.get();
            if (response != null && response.tokenUsage() != null) {
                usage = TokenUsage.of(
                    response.tokenUsage().inputTokenCount(),
                    response.tokenUsage().outputTokenCount()
                );
            }

            LlmResponse finalResponse = LlmResponse.success(
                request.id(), content, usage, getProviderName(), modelName, latency);
            handler.onComplete(finalResponse);

            log.debug("Gemini streaming complete: {} tokens in {}ms", usage.totalTokens(), latency.toMillis());
            return finalResponse;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "Streaming interrupted";
            handler.onError(e);
            return LlmResponse.error(request.id(), getProviderName(), errorMsg);
        } catch (Exception e) {
            log.error("Gemini streaming failed", e);
            handler.onError(e);
            return LlmResponse.error(request.id(), getProviderName(), e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public List<LlmCapability> getCapabilities() {
        return List.of(
            LlmCapability.TEXT_GENERATION,
            LlmCapability.CHAT,
            LlmCapability.FUNCTION_CALLING,
            LlmCapability.STREAMING,
            LlmCapability.VISION
        );
    }

    /**
     * Get the GCP project ID.
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Get the GCP location/region.
     */
    public String getLocation() {
        return location;
    }

    private List<ChatMessage> convertMessages(LlmRequest request) {
        List<ChatMessage> messages = new ArrayList<>();

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(SystemMessage.from(request.systemPrompt()));
        }

        if (request.messages() != null) {
            for (LlmRequest.Message msg : request.messages()) {
                messages.add(switch (msg.role()) {
                    case SYSTEM -> SystemMessage.from(msg.content());
                    case USER -> UserMessage.from(msg.content());
                    case ASSISTANT -> AiMessage.from(msg.content());
                    case TOOL -> UserMessage.from("[Tool Result]: " + msg.content());
                });
            }
        }

        if (request.prompt() != null && !request.prompt().isBlank()) {
            messages.add(UserMessage.from(request.prompt()));
        }

        return messages;
    }
}
