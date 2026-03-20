package com.regulus.platform.agents.a2a;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * REST controller exposing A2A server endpoints.
 * Implements the A2A protocol over HTTP/JSON-RPC.
 */
@RestController
public class A2aServerController {

    private static final Logger log = LoggerFactory.getLogger(A2aServerController.class);
    private static final String JSONRPC_VERSION = "2.0";

    private final A2aServer a2aServer;
    private final ObjectMapper objectMapper;

    public A2aServerController(A2aServer a2aServer, ObjectMapper objectMapper) {
        this.a2aServer = a2aServer;
        this.objectMapper = objectMapper;
        log.info("A2A server controller initialized");
    }

    /**
     * Serve the agent card at the well-known location.
     */
    @GetMapping(value = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getAgentCard() {
        A2aAgent agent = a2aServer.getAgentCard();

        Map<String, Object> card = new HashMap<>();
        card.put("id", agent.id());
        card.put("name", agent.name());
        card.put("description", agent.description());
        card.put("url", agent.url());
        card.put("provider", agent.provider());
        card.put("version", agent.version());

        List<Map<String, Object>> skillsList = agent.skills().stream()
            .map(skill -> {
                Map<String, Object> s = new HashMap<>();
                s.put("id", skill.id());
                s.put("name", skill.name());
                s.put("description", skill.description());
                s.put("inputSchema", skill.inputSchema());
                s.put("outputSchema", skill.outputSchema());
                return s;
            })
            .toList();
        card.put("skills", skillsList);

        if (!agent.authentication().isEmpty()) {
            card.put("authentication", agent.authentication());
        }
        if (!agent.metadata().isEmpty()) {
            card.put("metadata", agent.metadata());
        }

        log.debug("Serving agent card: {}", agent.name());
        return card;
    }

    /**
     * Handle task requests.
     */
    @PostMapping(value = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> handleTaskRequest(@RequestBody Map<String, Object> request) {
        String id = request.get("id") != null ? String.valueOf(request.get("id")) : null;
        String method = (String) request.get("method");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());

        log.debug("Received A2A request: method={}, id={}", method, id);

        return switch (method) {
            case "tasks/send" -> handleTaskSend(id, params);
            case "tasks/get" -> handleTaskGet(id, params);
            case "tasks/cancel" -> handleTaskCancel(id, params);
            default -> Mono.just(errorResponse(id, -32601, "Method not found: " + method));
        };
    }

    private Mono<Map<String, Object>> handleTaskSend(String requestId, Map<String, Object> params) {
        String taskId = (String) params.getOrDefault("id", UUID.randomUUID().toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) params.getOrDefault("message", Map.of());

        // Extract skill ID from message or params
        String skillId = (String) params.get("skillId");
        if (skillId == null) {
            skillId = extractSkillIdFromMessage(message);
        }

        if (skillId == null) {
            return Mono.just(errorResponse(requestId, -32602, "skillId is required"));
        }

        // Extract input from message parts
        Map<String, Object> input = extractInputFromMessage(message);

        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) params.getOrDefault("context", Map.of());

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) params.getOrDefault("metadata", Map.of());

        A2aClient.A2aTask task = A2aClient.A2aTask.builder()
            .id(taskId)
            .skillId(skillId)
            .input(input)
            .context(context)
            .metadata(metadata)
            .build();

        return Mono.fromFuture(a2aServer.executeTask(task))
            .map(response -> {
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
                return successResponse(requestId, result);
            })
            .onErrorResume(e -> {
                log.error("Task execution error: {}", e.getMessage());
                return Mono.just(errorResponse(requestId, -32000, e.getMessage()));
            });
    }

    private Mono<Map<String, Object>> handleTaskGet(String requestId, Map<String, Object> params) {
        String taskId = (String) params.get("id");
        if (taskId == null) {
            return Mono.just(errorResponse(requestId, -32602, "Task id is required"));
        }

        A2aClient.A2aTaskStatus status = a2aServer.getTaskStatus(taskId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", status.taskId());
        result.put("status", status.state().name().toLowerCase());
        result.put("progress", status.progress());
        result.put("message", status.message());

        return Mono.just(successResponse(requestId, result));
    }

    private Mono<Map<String, Object>> handleTaskCancel(String requestId, Map<String, Object> params) {
        String taskId = (String) params.get("id");
        if (taskId == null) {
            return Mono.just(errorResponse(requestId, -32602, "Task id is required"));
        }

        boolean cancelled = a2aServer.cancelTask(taskId);

        return Mono.just(successResponse(requestId, Map.of(
            "id", taskId,
            "cancelled", cancelled
        )));
    }

    private String extractSkillIdFromMessage(Map<String, Object> message) {
        // Try to extract skillId from message metadata or parts
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
                // Try to parse as JSON
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = objectMapper.readValue(text, Map.class);
                    input.putAll(parsed);
                } catch (Exception e) {
                    // Not JSON, treat as raw input
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
        if (id != null) {
            response.put("id", id);
        }
        response.put("result", result);
        return response;
    }

    private Map<String, Object> errorResponse(String id, int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        if (id != null) {
            response.put("id", id);
        }
        response.put("error", Map.of(
            "code", code,
            "message", message
        ));
        return response;
    }
}
