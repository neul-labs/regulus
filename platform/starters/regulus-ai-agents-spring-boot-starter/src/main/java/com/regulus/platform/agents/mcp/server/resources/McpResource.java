package com.regulus.platform.agents.mcp.server.resources;

import java.util.Map;

/**
 * Represents an MCP resource that can be discovered and read by clients.
 * Resources are context/data that can be provided to LLM interactions.
 */
public record McpResource(
    String uri,
    String name,
    String description,
    String mimeType,
    Map<String, Object> annotations
) {
    /**
     * Create a text resource.
     */
    public static McpResource text(String uri, String name, String description) {
        return new McpResource(uri, name, description, "text/plain", Map.of());
    }

    /**
     * Create a JSON resource.
     */
    public static McpResource json(String uri, String name, String description) {
        return new McpResource(uri, name, description, "application/json", Map.of());
    }

    /**
     * Create a resource with annotations.
     */
    public static McpResource of(String uri, String name, String description, String mimeType, Map<String, Object> annotations) {
        return new McpResource(uri, name, description, mimeType, annotations);
    }
}
