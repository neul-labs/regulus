package com.neullabs.regulus.agents.mcp.server.prompts;

import java.util.List;
import java.util.Map;

/**
 * Represents an MCP prompt template.
 * Prompts are reusable templates that can be expanded with arguments.
 */
public record McpPrompt(
    String name,
    String description,
    List<PromptArgument> arguments
) {
    /**
     * Create a prompt without arguments.
     */
    public static McpPrompt of(String name, String description) {
        return new McpPrompt(name, description, List.of());
    }

    /**
     * Create a prompt with arguments.
     */
    public static McpPrompt of(String name, String description, List<PromptArgument> arguments) {
        return new McpPrompt(name, description, arguments);
    }

    /**
     * Prompt argument definition.
     */
    public record PromptArgument(
        String name,
        String description,
        boolean required
    ) {
        public static PromptArgument required(String name, String description) {
            return new PromptArgument(name, description, true);
        }

        public static PromptArgument optional(String name, String description) {
            return new PromptArgument(name, description, false);
        }
    }
}
