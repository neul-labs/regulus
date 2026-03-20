package com.regulus.platform.agents.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE-based streaming controller for A2A protocol.
 * Implements streaming endpoints for long-running task executions and real-time updates.
 */
@RestController
@RequestMapping("/a2a/stream")
public class A2aStreamingController {

    private static final Logger log = LoggerFactory.getLogger(A2aStreamingController.class);
    private static final String JSONRPC_VERSION = "2.0";

    private final A2aServer a2aServer;
    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> taskStreams = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> clientStreams = new ConcurrentHashMap<>();

    public A2aStreamingController(A2aServer a2aServer, ObjectMapper objectMapper) {
        this.a2aServer = a2aServer;
        this.objectMapper = objectMapper;
        log.info("A2A streaming controller initialized");
    }

    /**
     * SSE endpoint for receiving real-time A2A events.
     * Clients connect here to receive streaming notifications.
     */
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents(
            @RequestParam(required = false) String clientId) {

        String resolvedClientId = clientId != null ? clientId : UUID.randomUUID().toString();
        log.info("Client connected to A2A SSE stream: {}", resolvedClientId);

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        clientStreams.put(resolvedClientId, sink);

        // Send initial connection event
        emitClientEvent(resolvedClientId, "connected", Map.of(
            "clientId", resolvedClientId,
            "agentInfo", a2aServer.getAgentCard()
        ));

        // Heartbeat every 30 seconds
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(30))
            .map(i -> ServerSentEvent.<String>builder()
                .event("heartbeat")
                .data("{\"timestamp\":" + System.currentTimeMillis() + "}")
                .build());

        return Flux.merge(sink.asFlux(), heartbeat)
            .doOnCancel(() -> {
                log.info("Client disconnected from A2A SSE stream: {}", resolvedClientId);
                clientStreams.remove(resolvedClientId);
            })
            .doOnError(e -> {
                log.error("A2A SSE stream error for client {}: {}", resolvedClientId, e.getMessage());
                clientStreams.remove(resolvedClientId);
            });
    }

    /**
     * Execute a task with streaming progress updates.
     * Subscribe to the task stream endpoint to receive real-time updates.
     */
    @PostMapping(path = "/tasks/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> sendTaskWithStreaming(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId) {

        String requestId = request.get("id") != null ? String.valueOf(request.get("id")) : UUID.randomUUID().toString();

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());

        String taskId = (String) params.getOrDefault("id", UUID.randomUUID().toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) params.getOrDefault("message", Map.of());

        String skillId = (String) params.get("skillId");
        if (skillId == null) {
            skillId = extractSkillIdFromMessage(message);
        }

        if (skillId == null) {
            return Mono.just(errorResponse(requestId, -32602, "skillId is required"));
        }

        Map<String, Object> input = extractInputFromMessage(message);

        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) params.getOrDefault("context", Map.of());

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) params.getOrDefault("metadata", Map.of());

        // Create a stream for this task
        Sinks.Many<ServerSentEvent<String>> taskSink = Sinks.many().multicast().onBackpressureBuffer();
        taskStreams.put(taskId, taskSink);

        // Emit task started event
        emitTaskEvent(taskId, "task_started", Map.of(
            "taskId", taskId,
            "skillId", skillId,
            "timestamp", System.currentTimeMillis()
        ));

        // Also emit to client if connected
        if (clientId != null) {
            emitClientEvent(clientId, "task_started", Map.of(
                "taskId", taskId,
                "skillId", skillId,
                "timestamp", System.currentTimeMillis()
            ));
        }

        A2aClient.A2aTask task = A2aClient.A2aTask.builder()
            .id(taskId)
            .skillId(skillId)
            .input(input)
            .context(context)
            .metadata(metadata)
            .build();

        String finalSkillId = skillId;
        String finalClientId = clientId;

        return Mono.fromFuture(a2aServer.executeTask(task))
            .map(response -> {
                // Emit completion event
                Map<String, Object> completionData = Map.of(
                    "taskId", response.taskId(),
                    "status", response.state().name().toLowerCase(),
                    "timestamp", System.currentTimeMillis()
                );

                emitTaskEvent(taskId, "task_completed", completionData);

                if (finalClientId != null) {
                    emitClientEvent(finalClientId, "task_completed", completionData);
                }

                // Clean up task stream
                taskStreams.remove(taskId);

                Map<String, Object> result = new HashMap<>();
                result.put("id", response.taskId());
                result.put("status", response.state().name().toLowerCase());
                if (response.result() != null) {
                    result.put("output", response.result());
                }
                if (response.error() != null) {
                    result.put("error", response.error());
                }
                result.put("metadata", response.metadata());
                result.put("streamEndpoint", "/a2a/stream/tasks/" + taskId);

                return successResponse(requestId, result);
            })
            .onErrorResume(e -> {
                log.error("Task execution error: {}", e.getMessage());

                Map<String, Object> errorData = Map.of(
                    "taskId", taskId,
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                );

                emitTaskEvent(taskId, "task_error", errorData);

                if (finalClientId != null) {
                    emitClientEvent(finalClientId, "task_error", errorData);
                }

                taskStreams.remove(taskId);

                return Mono.just(errorResponse(requestId, -32000, e.getMessage()));
            });
    }

    /**
     * Subscribe to a specific task's updates via SSE.
     */
    @GetMapping(path = "/tasks/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamTaskUpdates(@PathVariable String taskId) {
        log.info("Client subscribed to task stream: {}", taskId);

        Sinks.Many<ServerSentEvent<String>> sink = taskStreams.computeIfAbsent(
            taskId,
            id -> Sinks.many().multicast().onBackpressureBuffer()
        );

        // Get current task status
        A2aClient.A2aTaskStatus status = a2aServer.getTaskStatus(taskId);

        return Flux.concat(
            // Emit current status immediately
            Flux.just(createSseEvent("task_status", Map.of(
                "taskId", taskId,
                "status", status.state().name().toLowerCase(),
                "progress", status.progress(),
                "message", status.message() != null ? status.message() : "",
                "timestamp", System.currentTimeMillis()
            ))),
            // Then stream any updates
            sink.asFlux()
        ).doOnCancel(() -> {
            log.debug("Client unsubscribed from task stream: {}", taskId);
        });
    }

    /**
     * Send progress update for a task (used internally by task handlers).
     */
    public void emitTaskProgress(String taskId, int progress, String message) {
        emitTaskEvent(taskId, "task_progress", Map.of(
            "taskId", taskId,
            "progress", progress,
            "message", message,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Send an artifact/output chunk for a task (used during streaming output).
     */
    public void emitTaskArtifact(String taskId, String artifactType, Object data) {
        emitTaskEvent(taskId, "task_artifact", Map.of(
            "taskId", taskId,
            "type", artifactType,
            "data", data,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Broadcast an event to all connected clients.
     */
    public void broadcastEvent(String eventType, Map<String, Object> data) {
        clientStreams.forEach((clientId, sink) -> emitClientEvent(clientId, eventType, data));
    }

    private void emitTaskEvent(String taskId, String eventType, Map<String, Object> data) {
        Sinks.Many<ServerSentEvent<String>> sink = taskStreams.get(taskId);
        if (sink != null) {
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                    .event(eventType)
                    .data(jsonData)
                    .build();
                sink.tryEmitNext(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize task SSE event: {}", e.getMessage());
            }
        }
    }

    private void emitClientEvent(String clientId, String eventType, Map<String, Object> data) {
        Sinks.Many<ServerSentEvent<String>> sink = clientStreams.get(clientId);
        if (sink != null) {
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                    .event(eventType)
                    .data(jsonData)
                    .build();
                sink.tryEmitNext(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize client SSE event: {}", e.getMessage());
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

    private String extractSkillIdFromMessage(Map<String, Object> message) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>)
            message.getOrDefault("parts", List.of());

        for (Map<String, Object> part : parts) {
            if ("skill".equals(part.get("type"))) {
                return (String) part.get("skillId");
            }
        }
        return null;
    }

    private Map<String, Object> extractInputFromMessage(Map<String, Object> message) {
        Map<String, Object> input = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>)
            message.getOrDefault("parts", List.of());

        for (Map<String, Object> part : parts) {
            String type = (String) part.getOrDefault("type", "text");
            if ("text".equals(type)) {
                String text = (String) part.get("text");
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = objectMapper.readValue(text, Map.class);
                    input.putAll(parsed);
                } catch (Exception e) {
                    input.put("text", text);
                }
            } else if ("data".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) part.get("data");
                if (data != null) {
                    input.putAll(data);
                }
            }
        }

        return input;
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

    /**
     * Get number of active task streams.
     */
    public int getActiveTaskStreamCount() {
        return taskStreams.size();
    }

    /**
     * Get number of connected clients.
     */
    public int getConnectedClientCount() {
        return clientStreams.size();
    }
}
