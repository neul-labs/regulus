package com.regulus.platform.agents.mcp.server;

import com.regulus.platform.agents.mcp.McpTool;
import com.regulus.platform.agents.mcp.server.resources.McpResource;
import com.regulus.platform.agents.mcp.server.resources.McpResourceProvider;
import com.regulus.platform.agents.mcp.server.prompts.McpPrompt;
import com.regulus.platform.agents.mcp.server.prompts.McpPromptManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for MCP (Model Context Protocol) server implementations.
 * Exposes tools, resources, and prompts that can be discovered and invoked by MCP clients.
 */
public interface McpServer {

    /**
     * Get the server information.
     */
    ServerInfo getServerInfo();

    // ============ Tools API ============

    /**
     * Get all registered tools.
     */
    List<McpTool> getTools();

    /**
     * Execute a tool by name.
     *
     * @param toolName the name of the tool to execute
     * @param arguments the arguments for the tool
     * @return the tool execution result
     */
    CompletableFuture<ToolResult> executeTool(String toolName, Map<String, Object> arguments);

    /**
     * Register a tool handler.
     *
     * @param tool the tool definition
     * @param handler the handler to execute the tool
     */
    void registerTool(McpTool tool, ToolHandler handler);

    /**
     * Unregister a tool.
     *
     * @param toolName the name of the tool to unregister
     */
    void unregisterTool(String toolName);

    // ============ Resources API ============

    /**
     * List all available resources.
     *
     * @param cursor optional pagination cursor
     * @return list of resources
     */
    default McpResourceProvider.ResourceList listResources(String cursor) {
        return McpResourceProvider.ResourceList.of(List.of());
    }

    /**
     * Read a resource by URI.
     *
     * @param uri the resource URI
     * @return the resource content if found
     */
    default Optional<McpResourceProvider.ResourceContent> readResource(String uri) {
        return Optional.empty();
    }

    /**
     * Register a resource provider.
     *
     * @param provider the resource provider
     */
    default void registerResourceProvider(McpResourceProvider provider) {
    }

    // ============ Prompts API ============

    /**
     * List all available prompts.
     *
     * @param cursor optional pagination cursor
     * @return list of prompts
     */
    default McpPromptManager.PromptList listPrompts(String cursor) {
        return McpPromptManager.PromptList.of(List.of());
    }

    /**
     * Get a prompt by name.
     *
     * @param name the prompt name
     * @return the prompt if found
     */
    default Optional<McpPrompt> getPrompt(String name) {
        return Optional.empty();
    }

    /**
     * Execute a prompt with arguments.
     *
     * @param name the prompt name
     * @param arguments the arguments for the prompt
     * @return the prompt result if found
     */
    default Optional<McpPromptManager.PromptResult> executePrompt(String name, Map<String, Object> arguments) {
        return Optional.empty();
    }

    /**
     * Register a prompt with its handler.
     *
     * @param prompt the prompt definition
     * @param handler the handler to generate prompt messages
     */
    default void registerPrompt(McpPrompt prompt, McpPromptManager.PromptHandler handler) {
    }

    /**
     * Server information record.
     */
    record ServerInfo(
        String name,
        String version,
        Map<String, Object> capabilities
    ) {}

    /**
     * Tool execution result.
     */
    record ToolResult(
        List<ContentItem> content,
        boolean isError
    ) {
        public static ToolResult success(Object content) {
            return new ToolResult(List.of(ContentItem.text(String.valueOf(content))), false);
        }

        public static ToolResult success(String text) {
            return new ToolResult(List.of(ContentItem.text(text)), false);
        }

        public static ToolResult success(Map<String, Object> json) {
            return new ToolResult(List.of(ContentItem.json(json)), false);
        }

        public static ToolResult error(String message) {
            return new ToolResult(List.of(ContentItem.text(message)), true);
        }
    }

    /**
     * Content item in a tool result.
     */
    record ContentItem(
        String type,
        Object data
    ) {
        public static ContentItem text(String text) {
            return new ContentItem("text", text);
        }

        public static ContentItem json(Map<String, Object> json) {
            return new ContentItem("json", json);
        }

        public static ContentItem image(String base64Data, String mimeType) {
            return new ContentItem("image", Map.of("data", base64Data, "mimeType", mimeType));
        }
    }

    /**
     * Handler interface for tool execution.
     */
    @FunctionalInterface
    interface ToolHandler {
        ToolResult handle(Map<String, Object> arguments) throws Exception;
    }
}
