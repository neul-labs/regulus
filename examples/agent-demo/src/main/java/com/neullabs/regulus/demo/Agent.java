package com.neullabs.regulus.demo;

import com.neullabs.regulus.llm.LlmClient;
import com.neullabs.regulus.llm.LlmRequest;
import com.neullabs.regulus.llm.LlmResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple AI agent demonstrating the core agentic loop:
 *
 * 1. Receive user input
 * 2. Send to LLM with available tools
 * 3. If LLM requests tool calls, execute them and loop
 * 4. Return final response when LLM stops
 *
 * This demonstrates the fundamental pattern used in all AI agents.
 */
public class Agent {

    private final String name;
    private final String systemPrompt;
    private final LlmClient llmClient;
    private final Map<String, Tool> tools;
    private final List<LlmRequest.Message> conversationHistory;
    private final AgentEventListener eventListener;
    private final int maxIterations;

    private Agent(Builder builder) {
        this.name = builder.name;
        this.systemPrompt = builder.systemPrompt;
        this.llmClient = builder.llmClient;
        this.tools = new HashMap<>(builder.tools);
        this.conversationHistory = new ArrayList<>();
        this.eventListener = builder.eventListener != null
            ? builder.eventListener
            : new AgentEventListener() {};
        this.maxIterations = builder.maxIterations;
    }

    /**
     * Run the agent with the given user input.
     * Returns the final response after processing all tool calls.
     */
    public AgentResponse run(String userInput) {
        eventListener.onAgentStart(name, userInput);

        // Add user message to history
        conversationHistory.add(new LlmRequest.Message(
            LlmRequest.Message.Role.USER,
            userInput
        ));

        int iteration = 0;
        int totalToolCalls = 0;
        List<ToolExecution> allToolExecutions = new ArrayList<>();

        while (iteration < maxIterations) {
            iteration++;
            eventListener.onIteration(iteration);

            // Build the LLM request
            LlmRequest request = buildRequest();

            // Call the LLM
            eventListener.onLlmCall(request);
            LlmResponse response = llmClient.generate(request);
            eventListener.onLlmResponse(response);

            // Check for errors
            if (!response.success()) {
                return AgentResponse.error(response.errorMessage(), iteration, allToolExecutions);
            }

            // Check if LLM wants to call tools
            if (response.finishReason() == LlmResponse.FinishReason.TOOL_CALLS
                && !response.toolCalls().isEmpty()) {

                List<ToolExecution> executions = executeToolCalls(response.toolCalls());
                allToolExecutions.addAll(executions);
                totalToolCalls += executions.size();

                // Add tool results to conversation
                for (ToolExecution exec : executions) {
                    conversationHistory.add(new LlmRequest.Message(
                        LlmRequest.Message.Role.TOOL,
                        formatToolResult(exec)
                    ));
                }

                continue; // Loop back to call LLM again
            }

            // LLM is done - add assistant response to history
            conversationHistory.add(new LlmRequest.Message(
                LlmRequest.Message.Role.ASSISTANT,
                response.content()
            ));

            eventListener.onAgentComplete(response.content());

            return AgentResponse.success(
                response.content(),
                iteration,
                totalToolCalls,
                allToolExecutions
            );
        }

        // Max iterations reached
        return AgentResponse.error(
            "Max iterations reached without completion",
            iteration,
            allToolExecutions
        );
    }

    private LlmRequest buildRequest() {
        List<LlmRequest.Tool> toolDefinitions = tools.values().stream()
            .map(t -> new LlmRequest.Tool(
                t.getName(),
                t.getDescription(),
                t.getInputSchema()
            ))
            .toList();

        return LlmRequest.builder()
            .systemPrompt(systemPrompt)
            .messages(new ArrayList<>(conversationHistory))
            .tools(toolDefinitions)
            .build();
    }

    private List<ToolExecution> executeToolCalls(List<LlmResponse.ToolCall> toolCalls) {
        List<ToolExecution> executions = new ArrayList<>();

        for (LlmResponse.ToolCall call : toolCalls) {
            eventListener.onToolCall(call.name(), call.arguments());

            Tool tool = tools.get(call.name());
            Tool.ToolResult result;

            if (tool == null) {
                result = Tool.ToolResult.error("Unknown tool: " + call.name());
            } else {
                try {
                    result = tool.execute(call.arguments());
                } catch (Exception e) {
                    result = Tool.ToolResult.error("Tool execution failed: " + e.getMessage());
                }
            }

            eventListener.onToolResult(call.name(), result);
            executions.add(new ToolExecution(call.id(), call.name(), call.arguments(), result));
        }

        return executions;
    }

    private String formatToolResult(ToolExecution execution) {
        return String.format(
            "[Tool: %s] %s",
            execution.toolName(),
            execution.result().output()
        );
    }

    /**
     * Clear the conversation history to start fresh.
     */
    public void clearHistory() {
        conversationHistory.clear();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating Agent instances.
     */
    public static class Builder {
        private String name = "Agent";
        private String systemPrompt = "You are a helpful AI assistant.";
        private LlmClient llmClient;
        private final Map<String, Tool> tools = new HashMap<>();
        private AgentEventListener eventListener;
        private int maxIterations = 10;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder llmClient(LlmClient llmClient) {
            this.llmClient = llmClient;
            return this;
        }

        public Builder addTool(Tool tool) {
            this.tools.put(tool.getName(), tool);
            return this;
        }

        public Builder eventListener(AgentEventListener listener) {
            this.eventListener = listener;
            return this;
        }

        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Agent build() {
            if (llmClient == null) {
                throw new IllegalStateException("LlmClient is required");
            }
            return new Agent(this);
        }
    }

    /**
     * Response from an agent run.
     */
    public record AgentResponse(
        boolean success,
        String content,
        String errorMessage,
        int iterations,
        int toolCallCount,
        List<ToolExecution> toolExecutions
    ) {
        public static AgentResponse success(
                String content,
                int iterations,
                int toolCallCount,
                List<ToolExecution> toolExecutions) {
            return new AgentResponse(true, content, null, iterations, toolCallCount, toolExecutions);
        }

        public static AgentResponse error(
                String message,
                int iterations,
                List<ToolExecution> toolExecutions) {
            return new AgentResponse(false, null, message, iterations, 0, toolExecutions);
        }
    }

    /**
     * Record of a tool execution.
     */
    public record ToolExecution(
        String callId,
        String toolName,
        Map<String, Object> arguments,
        Tool.ToolResult result
    ) {}

    /**
     * Listener for agent events (useful for debugging and observability).
     */
    public interface AgentEventListener {
        default void onAgentStart(String agentName, String input) {}
        default void onIteration(int iteration) {}
        default void onLlmCall(LlmRequest request) {}
        default void onLlmResponse(LlmResponse response) {}
        default void onToolCall(String toolName, Map<String, Object> arguments) {}
        default void onToolResult(String toolName, Tool.ToolResult result) {}
        default void onAgentComplete(String finalResponse) {}
    }
}
