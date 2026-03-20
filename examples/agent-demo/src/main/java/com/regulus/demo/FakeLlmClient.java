package com.regulus.demo;

import com.regulus.platform.llm.LlmClient;
import com.regulus.platform.llm.LlmRequest;
import com.regulus.platform.llm.LlmResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Fake LLM client for demonstration purposes.
 * Implements the real Regulus LlmClient interface.
 * Simulates LLM behavior including tool calls without requiring a real API.
 */
public class FakeLlmClient implements LlmClient {

    private int turnCount = 0;
    private final List<FakeResponse> scriptedResponses;

    public FakeLlmClient(List<FakeResponse> scriptedResponses) {
        this.scriptedResponses = scriptedResponses;
    }

    /**
     * Create a default client with a scripted conversation demonstrating tool use.
     */
    public static FakeLlmClient withDefaultScript() {
        return new FakeLlmClient(List.of(
            // Turn 1: Agent decides to look up account balance
            new FakeResponse(
                null,
                List.of(new LlmResponse.ToolCall(
                    "call_001",
                    "get_account_balance",
                    Map.of("account_id", "ACC-12345")
                )),
                LlmResponse.FinishReason.TOOL_CALLS
            ),
            // Turn 2: Agent decides to check transaction history
            new FakeResponse(
                null,
                List.of(new LlmResponse.ToolCall(
                    "call_002",
                    "get_transactions",
                    Map.of("account_id", "ACC-12345", "limit", 5)
                )),
                LlmResponse.FinishReason.TOOL_CALLS
            ),
            // Turn 3: Agent provides final response
            new FakeResponse(
                """
                Based on my analysis:

                Your account ACC-12345 has a current balance of £2,450.00.

                Recent transactions:
                - Dec 10: Salary deposit +£3,000.00
                - Dec 9: Grocery store -£85.50
                - Dec 8: Electric bill -£120.00
                - Dec 7: Restaurant -£45.00
                - Dec 6: ATM withdrawal -£200.00

                Your account is in good standing. Would you like me to help with anything else?
                """,
                List.of(),
                LlmResponse.FinishReason.STOP
            )
        ));
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        // Simulate some latency
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (turnCount >= scriptedResponses.size()) {
            // Default response if we run out of scripted responses
            return LlmResponse.success(
                request.id(),
                "I've completed the task. Is there anything else you'd like me to help with?",
                LlmResponse.TokenUsage.of(50, 30),
                "fake",
                "fake-model-v1",
                Duration.ofMillis(100)
            );
        }

        FakeResponse scripted = scriptedResponses.get(turnCount++);

        if (!scripted.toolCalls().isEmpty()) {
            return LlmResponse.withToolCalls(
                request.id(),
                scripted.toolCalls(),
                LlmResponse.TokenUsage.of(100, 50),
                "fake",
                "fake-model-v1",
                Duration.ofMillis(100)
            );
        }

        return LlmResponse.success(
            request.id(),
            scripted.content(),
            LlmResponse.TokenUsage.of(150, 200),
            "fake",
            "fake-model-v1",
            Duration.ofMillis(100)
        );
    }

    @Override
    public CompletableFuture<LlmResponse> generateAsync(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> generate(request));
    }

    @Override
    public LlmResponse generateStreaming(LlmRequest request, StreamingHandler handler) {
        LlmResponse response = generate(request);
        if (response.content() != null) {
            // Simulate streaming by breaking content into words
            String[] words = response.content().split(" ");
            for (String word : words) {
                handler.onToken(word + " ");
            }
        }
        handler.onComplete(response);
        return response;
    }

    @Override
    public String getProviderName() {
        return "fake";
    }

    @Override
    public String getModelName() {
        return "fake-model-v1";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<LlmCapability> getCapabilities() {
        return List.of(
            LlmCapability.TEXT_GENERATION,
            LlmCapability.CHAT,
            LlmCapability.FUNCTION_CALLING,
            LlmCapability.STREAMING
        );
    }

    /**
     * Reset the conversation to start from the beginning.
     */
    public void reset() {
        turnCount = 0;
    }

    /**
     * A scripted response for the fake LLM.
     */
    public record FakeResponse(
        String content,
        List<LlmResponse.ToolCall> toolCalls,
        LlmResponse.FinishReason finishReason
    ) {}
}
