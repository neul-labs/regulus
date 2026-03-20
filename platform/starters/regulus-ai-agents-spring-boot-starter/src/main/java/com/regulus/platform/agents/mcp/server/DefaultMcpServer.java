package com.regulus.platform.agents.mcp.server;

import com.regulus.platform.agents.mcp.McpTool;
import com.regulus.platform.agents.mcp.server.prompts.McpPrompt;
import com.regulus.platform.agents.mcp.server.prompts.McpPromptManager;
import com.regulus.platform.agents.mcp.server.resources.McpResourceManager;
import com.regulus.platform.agents.mcp.server.resources.McpResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of MCP server.
 * Manages tool, resource, and prompt registration and execution.
 */
public class DefaultMcpServer implements McpServer {

    private static final Logger log = LoggerFactory.getLogger(DefaultMcpServer.class);

    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final String name;
    private final String version;
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    private final Map<String, ToolHandler> handlers = new ConcurrentHashMap<>();
    private final McpResourceManager resourceManager = new McpResourceManager();
    private final McpPromptManager promptManager = new McpPromptManager();

    public DefaultMcpServer(String name, String version) {
        this.name = name;
        this.version = version;
        log.info("Created MCP server: {} v{}", name, version);
    }

    @Override
    public ServerInfo getServerInfo() {
        return new ServerInfo(
            name,
            version,
            Map.of(
                "tools", Map.of("listChanged", false),
                "resources", Map.of(
                    "subscribe", true,
                    "listChanged", true
                ),
                "prompts", Map.of("listChanged", false),
                "protocolVersion", PROTOCOL_VERSION
            )
        );
    }

    @Override
    public List<McpTool> getTools() {
        return new ArrayList<>(tools.values());
    }

    @Override
    public CompletableFuture<ToolResult> executeTool(String toolName, Map<String, Object> arguments) {
        return CompletableFuture.supplyAsync(() -> {
            McpTool tool = tools.get(toolName);
            if (tool == null) {
                log.warn("Tool '{}' not found", toolName);
                return ToolResult.error("Tool not found: " + toolName);
            }

            ToolHandler handler = handlers.get(toolName);
            if (handler == null) {
                log.warn("No handler registered for tool '{}'", toolName);
                return ToolResult.error("No handler for tool: " + toolName);
            }

            try {
                log.debug("Executing tool '{}' with arguments: {}", toolName, arguments);
                ToolResult result = handler.handle(arguments);
                log.debug("Tool '{}' executed successfully", toolName);
                return result;
            } catch (Exception e) {
                log.error("Error executing tool '{}': {}", toolName, e.getMessage(), e);
                return ToolResult.error("Tool execution failed: " + e.getMessage());
            }
        });
    }

    @Override
    public void registerTool(McpTool tool, ToolHandler handler) {
        tools.put(tool.name(), tool);
        handlers.put(tool.name(), handler);
        log.info("Registered tool: {} - {}", tool.name(), tool.description());
    }

    @Override
    public void unregisterTool(String toolName) {
        tools.remove(toolName);
        handlers.remove(toolName);
        log.info("Unregistered tool: {}", toolName);
    }

    /**
     * Get the number of registered tools.
     */
    public int getToolCount() {
        return tools.size();
    }

    // ============ Resources Implementation ============

    @Override
    public McpResourceProvider.ResourceList listResources(String cursor) {
        return resourceManager.listResources(cursor);
    }

    @Override
    public Optional<McpResourceProvider.ResourceContent> readResource(String uri) {
        return resourceManager.readResource(uri);
    }

    @Override
    public void registerResourceProvider(McpResourceProvider provider) {
        resourceManager.registerProvider(provider);
    }

    /**
     * Get the resource manager for direct access.
     */
    public McpResourceManager getResourceManager() {
        return resourceManager;
    }

    // ============ Prompts Implementation ============

    @Override
    public McpPromptManager.PromptList listPrompts(String cursor) {
        return promptManager.listPrompts(cursor);
    }

    @Override
    public Optional<McpPrompt> getPrompt(String name) {
        return promptManager.getPrompt(name);
    }

    @Override
    public Optional<McpPromptManager.PromptResult> executePrompt(String name, Map<String, Object> arguments) {
        return promptManager.executePrompt(name, arguments);
    }

    @Override
    public void registerPrompt(McpPrompt prompt, McpPromptManager.PromptHandler handler) {
        promptManager.registerPrompt(prompt, handler);
    }

    /**
     * Get the prompt manager for direct access.
     */
    public McpPromptManager getPromptManager() {
        return promptManager;
    }

    /**
     * Get the number of registered resources.
     */
    public int getResourceProviderCount() {
        return resourceManager.getProviderCount();
    }

    /**
     * Get the number of registered prompts.
     */
    public int getPromptCount() {
        return promptManager.getPromptCount();
    }
}
