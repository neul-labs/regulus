package com.neullabs.regulus.demo;

import java.util.Map;

/**
 * A tool that can be invoked by an agent.
 * Tools extend the agent's capabilities beyond text generation.
 */
public interface Tool {

    /**
     * Get the unique name of this tool.
     */
    String getName();

    /**
     * Get a description of what this tool does.
     * This is provided to the LLM to help it decide when to use the tool.
     */
    String getDescription();

    /**
     * Get the JSON schema for the tool's input parameters.
     */
    Map<String, Object> getInputSchema();

    /**
     * Execute the tool with the given arguments.
     *
     * @param arguments the arguments provided by the LLM
     * @return the result of the tool execution
     */
    ToolResult execute(Map<String, Object> arguments);

    /**
     * Result of a tool execution.
     */
    record ToolResult(
        boolean success,
        String output,
        Map<String, Object> data
    ) {
        public static ToolResult success(String output) {
            return new ToolResult(true, output, Map.of());
        }

        public static ToolResult success(String output, Map<String, Object> data) {
            return new ToolResult(true, output, data);
        }

        public static ToolResult error(String message) {
            return new ToolResult(false, message, Map.of());
        }
    }
}
