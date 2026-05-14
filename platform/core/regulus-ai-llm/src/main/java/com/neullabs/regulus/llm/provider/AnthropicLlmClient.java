package com.neullabs.regulus.llm.provider;

import com.neullabs.regulus.llm.LlmClient;
import com.neullabs.regulus.llm.LlmRequest;
import com.neullabs.regulus.llm.LlmResponse;
import com.neullabs.regulus.llm.LlmResponse.TokenUsage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.StreamingResponseHandler;
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
 * Anthropic Claude LLM client implementation using LangChain4j.
 */
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);
    private static final long STREAMING_TIMEOUT_SECONDS = 300; // 5 minutes

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final String modelName;
    private final boolean available;

    public AnthropicLlmClient(String apiKey, String modelName, Duration timeout) {
        this.modelName = modelName;
        if (apiKey != null && !apiKey.isBlank()) {
            this.chatModel = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(timeout)
                .logRequests(true)
                .logResponses(true)
                .build();

            this.streamingChatModel = AnthropicStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(timeout)
                .logRequests(true)
                .logResponses(true)
                .build();

            this.available = true;
            log.info("Anthropic client initialized with streaming: model={}", modelName);
        } else {
            this.chatModel = null;
            this.streamingChatModel = null;
            this.available = false;
            log.warn("Anthropic client not available - no API key provided");
        }
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (!available) {
            return LlmResponse.unavailable(getProviderName(), "API key not configured");
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

            log.debug("Anthropic response: {} tokens in {}ms", usage.totalTokens(), latency.toMillis());

            return LlmResponse.success(request.id(), content, usage, getProviderName(), modelName, latency);

        } catch (Exception e) {
            log.error("Anthropic request failed", e);
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
            LlmResponse error = LlmResponse.unavailable(getProviderName(), "API key not configured");
            handler.onError(new IllegalStateException("Anthropic not available"));
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
                    log.error("Anthropic streaming error", error);
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

            log.debug("Anthropic streaming complete: {} tokens in {}ms", usage.totalTokens(), latency.toMillis());
            return finalResponse;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "Streaming interrupted";
            handler.onError(e);
            return LlmResponse.error(request.id(), getProviderName(), errorMsg);
        } catch (Exception e) {
            log.error("Anthropic streaming failed", e);
            handler.onError(e);
            return LlmResponse.error(request.id(), getProviderName(), e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "anthropic";
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
