package com.regulus.platform.agents.mcp;

import java.util.Map;

/**
 * Response from an MCP tool invocation.
 */
public record McpToolResponse(
    boolean success,
    Object content,
    String errorMessage,
    Map<String, Object> metadata
) {

    public static McpToolResponse success(Object content) {
        return new McpToolResponse(true, content, null, Map.of());
    }

    public static McpToolResponse success(Object content, Map<String, Object> metadata) {
        return new McpToolResponse(true, content, null, metadata);
    }

    public static McpToolResponse error(String errorMessage) {
        return new McpToolResponse(false, null, errorMessage, Map.of());
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }
}
