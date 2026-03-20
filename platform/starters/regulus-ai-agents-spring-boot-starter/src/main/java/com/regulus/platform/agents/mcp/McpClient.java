package com.regulus.platform.agents.mcp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Client interface for Model Context Protocol (MCP) servers.
 * Enables tool discovery and invocation across MCP-compliant services.
 */
public interface McpClient {

    /**
     * Discover available tools from the configured MCP servers.
     */
    CompletableFuture<List<McpTool>> discoverTools();

    /**
     * Invoke a specific tool by name with the given arguments.
     *
     * @param toolName the name of the tool to invoke
     * @param arguments the arguments to pass to the tool
     * @return the tool's response
     */
    CompletableFuture<McpToolResponse> invoke(String toolName, Map<String, Object> arguments);

    /**
     * Check if the client is connected to the MCP server.
     */
    boolean isConnected();

    /**
     * Connect to the MCP server.
     */
    CompletableFuture<Void> connect();

    /**
     * Disconnect from the MCP server.
     */
    CompletableFuture<Void> disconnect();

    /**
     * Get the server URL this client connects to.
     */
    String getServerUrl();
}
