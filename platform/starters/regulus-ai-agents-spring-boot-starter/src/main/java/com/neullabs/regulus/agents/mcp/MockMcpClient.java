package com.neullabs.regulus.agents.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of MCP client for development and testing.
 * Simulates tool discovery and invocation without requiring a real MCP server.
 */
public class MockMcpClient implements McpClient {

    private static final Logger log = LoggerFactory.getLogger(MockMcpClient.class);

    private final String serverUrl;
    private final Map<String, McpTool> registeredTools;
    private final Map<String, ToolHandler> toolHandlers;
    private boolean connected;

    public MockMcpClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.registeredTools = new ConcurrentHashMap<>();
        this.toolHandlers = new ConcurrentHashMap<>();
        this.connected = false;

        // Register default mock tools
        registerDefaultTools();
    }

    @Override
    public CompletableFuture<List<McpTool>> discoverTools() {
        if (!connected) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("MCP client not connected"));
        }

        log.debug("Discovering {} mock MCP tools from {}", registeredTools.size(), serverUrl);
        return CompletableFuture.completedFuture(new ArrayList<>(registeredTools.values()));
    }

    @Override
    public CompletableFuture<McpToolResponse> invoke(String toolName, Map<String, Object> arguments) {
        if (!connected) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("MCP client not connected"));
        }

        McpTool tool = registeredTools.get(toolName);
        if (tool == null) {
            log.warn("Tool '{}' not found in mock MCP server", toolName);
            return CompletableFuture.completedFuture(
                McpToolResponse.error("Tool not found: " + toolName));
        }

        ToolHandler handler = toolHandlers.get(toolName);
        if (handler != null) {
            try {
                log.debug("Invoking mock tool '{}' with arguments: {}", toolName, arguments);
                Object result = handler.handle(arguments);
                return CompletableFuture.completedFuture(
                    McpToolResponse.success(result, Map.of("mock", true)));
            } catch (Exception e) {
                log.error("Error invoking mock tool '{}': {}", toolName, e.getMessage());
                return CompletableFuture.completedFuture(
                    McpToolResponse.error(e.getMessage()));
            }
        }

        // Default mock response
        return CompletableFuture.completedFuture(
            McpToolResponse.success(
                Map.of("message", "Mock response for " + toolName),
                Map.of("mock", true, "tool", toolName)
            ));
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public CompletableFuture<Void> connect() {
        log.info("Connecting mock MCP client to {}", serverUrl);
        connected = true;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        log.info("Disconnecting mock MCP client from {}", serverUrl);
        connected = false;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Register a custom tool with a handler.
     */
    public void registerTool(McpTool tool, ToolHandler handler) {
        registeredTools.put(tool.name(), tool);
        if (handler != null) {
            toolHandlers.put(tool.name(), handler);
        }
        log.debug("Registered mock tool: {}", tool.name());
    }

    /**
     * Handler interface for mock tool implementations.
     */
    @FunctionalInterface
    public interface ToolHandler {
        Object handle(Map<String, Object> arguments) throws Exception;
    }

    private void registerDefaultTools() {
        // ISO 20022 Payment Validator (mock)
        registerTool(
            McpTool.builder()
                .name("iso20022_validate")
                .description("Validates ISO 20022 payment messages for compliance")
                .serverUrl(serverUrl)
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "message", Map.of("type", "string", "description", "ISO 20022 XML message"),
                        "messageType", Map.of("type", "string", "description", "Message type (e.g., pain.001)")
                    ),
                    "required", List.of("message", "messageType")
                ))
                .build(),
            args -> {
                String messageType = (String) args.getOrDefault("messageType", "unknown");
                return Map.of(
                    "valid", true,
                    "messageType", messageType,
                    "validationResults", List.of(
                        Map.of("rule", "schema_validation", "status", "passed"),
                        Map.of("rule", "business_rules", "status", "passed")
                    )
                );
            }
        );

        // Risk Scoring Tool (mock)
        registerTool(
            McpTool.builder()
                .name("risk_score")
                .description("Calculates risk score for transactions")
                .serverUrl(serverUrl)
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "transactionId", Map.of("type", "string"),
                        "amount", Map.of("type", "number"),
                        "currency", Map.of("type", "string")
                    )
                ))
                .build(),
            args -> {
                double amount = ((Number) args.getOrDefault("amount", 0)).doubleValue();
                int score = amount > 10000 ? 75 : (amount > 1000 ? 50 : 25);
                return Map.of(
                    "score", score,
                    "riskLevel", score > 70 ? "HIGH" : (score > 40 ? "MEDIUM" : "LOW"),
                    "factors", List.of("amount", "velocity", "geography")
                );
            }
        );

        // Customer Lookup Tool (mock)
        registerTool(
            McpTool.builder()
                .name("customer_lookup")
                .description("Looks up customer information by ID")
                .serverUrl(serverUrl)
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "customerId", Map.of("type", "string")
                    ),
                    "required", List.of("customerId")
                ))
                .build(),
            args -> Map.of(
                "customerId", args.get("customerId"),
                "name", "[REDACTED]",
                "segment", "RETAIL",
                "riskRating", "STANDARD",
                "kycStatus", "VERIFIED"
            )
        );

        log.info("Registered {} default mock MCP tools", registeredTools.size());
    }
}
