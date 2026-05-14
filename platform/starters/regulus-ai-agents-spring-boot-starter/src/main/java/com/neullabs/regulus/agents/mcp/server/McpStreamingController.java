package com.neullabs.regulus.agents.mcp.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neullabs.regulus.agents.mcp.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE-based streaming controller for MCP protocol.
 * Implements streaming endpoints for long-running tool executions and real-time updates.
 */
@RestController
@RequestMapping("${regulus.ai.mcp.server.path:/mcp}/stream")
public class McpStreamingController {

    private static final Logger log = LoggerFactory.getLogger(McpStreamingController.class);
    private static final String JSONRPC_VERSION = "2.0";

    private final McpServer mcpServer;
    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> clientSinks = new ConcurrentHashMap<>();

    public McpStreamingController(McpServer mcpServer, ObjectMapper objectMapper) {
        this.mcpServer = mcpServer;
        this.objectMapper = objectMapper;
        log.info("MCP streaming controller initialized");
    }

    /**
     * SSE endpoint for receiving real-time MCP events.
     * Clients connect here to receive streaming notifications.
     */
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents(
            @RequestParam(required = false) String clientId) {

        String resolvedClientId = clientId != null ? clientId : UUID.randomUUID().toString();
        log.info("Client connected to SSE stream: {}", resolvedClientId);

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        clientSinks.put(resolvedClientId, sink);

        // Send initial connection event
        emitEvent(resolvedClientId, "connected", Map.of(
            "clientId", resolvedClientId,
            "serverInfo", mcpServer.getServerInfo()
        ));

        // Heartbeat every 30 seconds to keep connection alive
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(30))
            .map(i -> ServerSentEvent.<String>builder()
                .event("heartbeat")
                .data("{\"timestamp\":" + System.currentTimeMillis() + "}")
                .build());

        return Flux.merge(sink.asFlux(), heartbeat)
            .doOnCancel(() -> {
                log.info("Client disconnected from SSE stream: {}", resolvedClientId);
                clientSinks.remove(resolvedClientId);
            })
            .doOnError(e -> {
                log.error("SSE stream error for client {}: {}", resolvedClientId, e.getMessage());
                clientSinks.remove(resolvedClientId);
            });
    }

    /**
     * Execute a tool with streaming progress updates.
     */
    @PostMapping(path = "/tools/call", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> executeToolWithStreaming(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId) {

        String id = request.get("id") != null ? String.valueOf(request.get("id")) : UUID.randomUUID().toString();

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());

        String toolName = (String) params.get("name");

        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

        log.debug("Streaming tool call '{}' with id: {}", toolName, id);

        // Emit progress start if client is connected
        if (clientId != null && clientSinks.containsKey(clientId)) {
            emitEvent(clientId, "tool_start", Map.of(
                "requestId", id,
                "toolName", toolName,
                "timestamp", System.currentTimeMillis()
            ));
        }

        return Mono.fromFuture(mcpServer.executeTool(toolName, arguments))
            .map(result -> {
                // Emit completion event
                if (clientId != null && clientSinks.containsKey(clientId)) {
                    emitEvent(clientId, "tool_complete", Map.of(
                        "requestId", id,
                        "toolName", toolName,
                        "isError", result.isError(),
                        "timestamp", System.currentTimeMillis()
                    ));
                }

                List<Map<String, Object>> content = result.content().stream()
                    .map(this::contentItemToMap)
                    .toList();

                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("content", content);
                resultMap.put("isError", result.isError());

                return successResponse(id, resultMap);
            })
            .onErrorResume(e -> {
                log.error("Streaming tool execution error: {}", e.getMessage());

                if (clientId != null && clientSinks.containsKey(clientId)) {
                    emitEvent(clientId, "tool_error", Map.of(
                        "requestId", id,
                        "toolName", toolName,
                        "error", e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    ));
                }

                return Mono.just(errorResponse(id, -32000, e.getMessage()));
            });
    }

    /**
     * Stream tool execution results as SSE events.
     * Returns a stream of progress events followed by the final result.
     */
    @GetMapping(path = "/tools/execute/{toolName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamToolExecution(
            @PathVariable String toolName,
            @RequestParam Map<String, Object> arguments) {

        String requestId = UUID.randomUUID().toString();
        log.debug("Streaming tool execution: {} with id: {}", toolName, requestId);

        return Flux.create(emitter -> {
            // Emit start event
            emitter.next(createSseEvent("start", Map.of(
                "requestId", requestId,
                "toolName", toolName,
                "timestamp", System.currentTimeMillis()
            )));

            // Execute tool
            mcpServer.executeTool(toolName, arguments)
                .thenAccept(result -> {
                    // Emit result event
                    List<Map<String, Object>> content = result.content().stream()
                        .map(this::contentItemToMap)
                        .toList();

                    emitter.next(createSseEvent("result", Map.of(
                        "requestId", requestId,
                        "content", content,
                        "isError", result.isError(),
                        "timestamp", System.currentTimeMillis()
                    )));

                    // Emit completion event
                    emitter.next(createSseEvent("complete", Map.of(
                        "requestId", requestId,
                        "timestamp", System.currentTimeMillis()
                    )));

                    emitter.complete();
                })
                .exceptionally(e -> {
                    emitter.next(createSseEvent("error", Map.of(
                        "requestId", requestId,
                        "error", e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    )));
                    emitter.complete();
                    return null;
                });
        });
    }

    /**
     * Broadcast an event to all connected clients.
     */
    public void broadcastEvent(String eventType, Map<String, Object> data) {
        clientSinks.forEach((clientId, sink) -> emitEvent(clientId, eventType, data));
    }

    /**
     * Emit an event to a specific client.
     */
    private void emitEvent(String clientId, String eventType, Map<String, Object> data) {
        Sinks.Many<ServerSentEvent<String>> sink = clientSinks.get(clientId);
        if (sink != null) {
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                    .event(eventType)
                    .data(jsonData)
                    .build();
                sink.tryEmitNext(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize SSE event: {}", e.getMessage());
            }
        }
    }

    private ServerSentEvent<String> createSseEvent(String eventType, Map<String, Object> data) {
        try {
            return ServerSentEvent.<String>builder()
                .event(eventType)
                .data(objectMapper.writeValueAsString(data))
                .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SSE event: {}", e.getMessage());
            return ServerSentEvent.<String>builder()
                .event("error")
                .data("{\"error\":\"Serialization failed\"}")
                .build();
        }
    }

    private Map<String, Object> contentItemToMap(McpServer.ContentItem item) {
        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("type", item.type());
        if ("text".equals(item.type())) {
            contentItem.put("text", item.data());
        } else if ("json".equals(item.type())) {
            try {
                contentItem.put("text", objectMapper.writeValueAsString(item.data()));
            } catch (JsonProcessingException e) {
                contentItem.put("text", String.valueOf(item.data()));
            }
        } else {
            contentItem.putAll(asMap(item.data()));
        }
        return contentItem;
    }

    private Map<String, Object> successResponse(String id, Map<String, Object> result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> errorResponse(String id, int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("error", Map.of(
            "code", code,
            "message", message
        ));
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Map.of("data", obj);
    }

    /**
     * Get the number of connected clients.
     */
    public int getConnectedClientCount() {
        return clientSinks.size();
    }
}
