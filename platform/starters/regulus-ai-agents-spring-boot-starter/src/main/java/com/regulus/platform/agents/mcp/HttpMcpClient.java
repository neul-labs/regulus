package com.regulus.platform.agents.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP-based MCP (Model Context Protocol) client implementation.
 * Communicates with MCP servers over HTTP/JSON-RPC following the MCP specification.
 */
public class HttpMcpClient implements McpClient {

    private static final Logger log = LoggerFactory.getLogger(HttpMcpClient.class);

    private static final String MCP_VERSION = "2024-11-05";
    private static final String JSONRPC_VERSION = "2.0";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final String serverUrl;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Map<String, McpTool> discoveredTools = new ConcurrentHashMap<>();

    // Server capabilities learned during initialization
    private Map<String, Object> serverCapabilities = Map.of();
    private String serverName;
    private String serverVersion;

    public HttpMcpClient(String serverUrl) {
        this(serverUrl, DEFAULT_TIMEOUT);
    }

    public HttpMcpClient(String serverUrl, Duration timeout) {
        this.serverUrl = serverUrl;
        this.timeout = timeout;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
            .baseUrl(serverUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
        log.info("Created HTTP MCP client for {}", serverUrl);
    }

    /**
     * Create client with custom WebClient (for mTLS, custom auth, etc.)
     */
    public HttpMcpClient(String serverUrl, WebClient webClient, Duration timeout) {
        this.serverUrl = serverUrl;
        this.webClient = webClient;
        this.timeout = timeout;
        this.objectMapper = new ObjectMapper();
        log.info("Created HTTP MCP client with custom WebClient for {}", serverUrl);
    }

    @Override
    public CompletableFuture<Void> connect() {
        log.info("Connecting to MCP server at {}", serverUrl);

        return sendRequest("initialize", Map.of(
            "protocolVersion", MCP_VERSION,
            "capabilities", Map.of(
                "tools", Map.of()
            ),
            "clientInfo", Map.of(
                "name", "regulus-mcp-client",
                "version", "1.0.0"
            )
        ))
        .doOnNext(response -> {
            if (response.containsKey("result")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                this.serverCapabilities = getMapOrEmpty(result, "capabilities");

                @SuppressWarnings("unchecked")
                Map<String, Object> serverInfo = (Map<String, Object>) result.getOrDefault("serverInfo", Map.of());
                this.serverName = (String) serverInfo.getOrDefault("name", "unknown");
                this.serverVersion = (String) serverInfo.getOrDefault("version", "unknown");

                log.info("Connected to MCP server: {} v{}", serverName, serverVersion);
                connected.set(true);
            } else if (response.containsKey("error")) {
                throw new McpException("Failed to initialize: " + response.get("error"));
            }
        })
        .then(sendNotification("initialized", Map.of()))
        .doOnSuccess(v -> log.debug("Sent initialized notification"))
        .toFuture();
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        log.info("Disconnecting from MCP server at {}", serverUrl);
        connected.set(false);
        discoveredTools.clear();
        serverCapabilities = Map.of();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    @Override
    public CompletableFuture<List<McpTool>> discoverTools() {
        if (!connected.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("MCP client not connected"));
        }

        log.debug("Discovering tools from MCP server");

        return sendRequest("tools/list", Map.of())
            .map(response -> {
                if (response.containsKey("error")) {
                    throw new McpException("Failed to list tools: " + response.get("error"));
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> toolsList = (List<Map<String, Object>>)
                    result.getOrDefault("tools", List.of());

                List<McpTool> tools = toolsList.stream()
                    .map(this::parseToolDefinition)
                    .toList();

                // Cache discovered tools
                discoveredTools.clear();
                tools.forEach(t -> discoveredTools.put(t.name(), t));

                log.info("Discovered {} tools from MCP server", tools.size());
                return tools;
            })
            .toFuture();
    }

    @Override
    public CompletableFuture<McpToolResponse> invoke(String toolName, Map<String, Object> arguments) {
        if (!connected.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("MCP client not connected"));
        }

        log.debug("Invoking tool '{}' with arguments: {}", toolName, arguments);

        return sendRequest("tools/call", Map.of(
            "name", toolName,
            "arguments", arguments
        ))
        .map(response -> {
            if (response.containsKey("error")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> error = (Map<String, Object>) response.get("error");
                String errorMessage = (String) error.getOrDefault("message", "Unknown error");
                log.warn("Tool '{}' invocation failed: {}", toolName, errorMessage);
                return McpToolResponse.error(errorMessage);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>)
                result.getOrDefault("content", List.of());

            // Process content based on type
            Object processedContent = processToolContent(content);

            boolean isError = (Boolean) result.getOrDefault("isError", false);
            if (isError) {
                return McpToolResponse.error(String.valueOf(processedContent));
            }

            log.debug("Tool '{}' invocation successful", toolName);
            return McpToolResponse.success(processedContent, Map.of(
                "server", serverName,
                "serverUrl", serverUrl
            ));
        })
        .onErrorResume(e -> {
            log.error("Error invoking tool '{}': {}", toolName, e.getMessage());
            return Mono.just(McpToolResponse.error(e.getMessage()));
        })
        .toFuture();
    }

    /**
     * Get discovered tool by name.
     */
    public McpTool getTool(String name) {
        return discoveredTools.get(name);
    }

    /**
     * Get server capabilities.
     */
    public Map<String, Object> getServerCapabilities() {
        return serverCapabilities;
    }

    /**
     * Get server name.
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Get server version.
     */
    public String getServerVersion() {
        return serverVersion;
    }

    // ==================== Private Methods ====================

    private Mono<Map<String, Object>> sendRequest(String method, Map<String, Object> params) {
        String id = String.valueOf(System.currentTimeMillis());
        Map<String, Object> request = Map.of(
            "jsonrpc", JSONRPC_VERSION,
            "id", id,
            "method", method,
            "params", params
        );

        return webClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(serializeRequest(request))
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(this::parseResponse)
            .doOnError(WebClientResponseException.class, e ->
                log.error("MCP request failed: {} {}", e.getStatusCode(), e.getMessage()))
            .doOnError(e ->
                log.error("MCP request error: {}", e.getMessage()));
    }

    private Mono<Void> sendNotification(String method, Map<String, Object> params) {
        Map<String, Object> notification = Map.of(
            "jsonrpc", JSONRPC_VERSION,
            "method", method,
            "params", params
        );

        return webClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(serializeRequest(notification))
            .retrieve()
            .bodyToMono(Void.class)
            .timeout(timeout);
    }

    private String serializeRequest(Map<String, Object> request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new McpException("Failed to serialize request", e);
        }
    }

    private Map<String, Object> parseResponse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new McpException("Failed to parse response", e);
        }
    }

    private McpTool parseToolDefinition(Map<String, Object> toolDef) {
        String name = (String) toolDef.get("name");
        String description = (String) toolDef.getOrDefault("description", "");

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>)
            toolDef.getOrDefault("inputSchema", Map.of());

        return McpTool.builder()
            .name(name)
            .description(description)
            .inputSchema(inputSchema)
            .serverUrl(serverUrl)
            .build();
    }

    private Object processToolContent(List<Map<String, Object>> content) {
        if (content.isEmpty()) {
            return null;
        }

        // If single content item, return its content directly
        if (content.size() == 1) {
            Map<String, Object> item = content.get(0);
            String type = (String) item.getOrDefault("type", "text");

            if ("text".equals(type)) {
                return item.get("text");
            } else if ("image".equals(type)) {
                return item; // Return full image object
            } else if ("resource".equals(type)) {
                return item.get("resource");
            }
            return item;
        }

        // Multiple content items - return as list
        return content;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapOrEmpty(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Map.of();
    }

    /**
     * Exception for MCP protocol errors.
     */
    public static class McpException extends RuntimeException {
        public McpException(String message) {
            super(message);
        }

        public McpException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
