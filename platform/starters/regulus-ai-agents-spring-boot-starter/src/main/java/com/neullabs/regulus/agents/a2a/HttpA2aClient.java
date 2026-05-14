package com.neullabs.regulus.agents.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP-based implementation of A2A client.
 * Communicates with A2A agents over HTTP/JSON following the A2A protocol.
 */
public class HttpA2aClient implements A2aClient {

    private static final Logger log = LoggerFactory.getLogger(HttpA2aClient.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final String A2A_VERSION = "0.1";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public HttpA2aClient() {
        this(DEFAULT_TIMEOUT);
    }

    public HttpA2aClient(Duration timeout) {
        this.timeout = timeout;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
        log.info("Created HTTP A2A client (timeout={})", timeout);
    }

    public HttpA2aClient(WebClient webClient, Duration timeout) {
        this.webClient = webClient;
        this.timeout = timeout;
        this.objectMapper = new ObjectMapper();
        log.info("Created HTTP A2A client with custom WebClient (timeout={})", timeout);
    }

    @Override
    public CompletableFuture<A2aAgent> discoverAgent(String agentUrl) {
        log.debug("Discovering agent at {}", agentUrl);

        // A2A agents expose their card at /.well-known/agent.json
        String cardUrl = normalizeUrl(agentUrl) + "/.well-known/agent.json";

        return webClient.get()
            .uri(cardUrl)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(json -> parseAgentCard(json, agentUrl))
            .doOnNext(agent -> log.info("Discovered agent: {} ({})", agent.name(), agent.id()))
            .toFuture();
    }

    @Override
    public CompletableFuture<A2aTaskResponse> sendTask(String agentUrl, A2aTask task) {
        log.debug("Sending task {} to agent at {}", task.id(), agentUrl);

        String tasksUrl = normalizeUrl(agentUrl) + "/tasks";

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", task.id());
        request.put("method", "tasks/send");
        request.put("params", Map.of(
            "id", task.id(),
            "message", Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                    "type", "text",
                    "text", serializeInput(task.input())
                ))
            ),
            "metadata", task.metadata()
        ));

        return webClient.post()
            .uri(tasksUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(serialize(request))
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(json -> parseTaskResponse(json, task.id()))
            .doOnNext(response -> log.debug("Task {} state: {}", task.id(), response.state()))
            .toFuture();
    }

    @Override
    public CompletableFuture<A2aTaskStatus> getTaskStatus(String agentUrl, String taskId) {
        log.debug("Getting status of task {} from {}", taskId, agentUrl);

        String statusUrl = normalizeUrl(agentUrl) + "/tasks/" + taskId;

        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", taskId,
            "method", "tasks/get",
            "params", Map.of("id", taskId)
        );

        return webClient.post()
            .uri(statusUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(serialize(request))
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(json -> parseTaskStatus(json, taskId))
            .toFuture();
    }

    @Override
    public CompletableFuture<Boolean> cancelTask(String agentUrl, String taskId) {
        log.debug("Cancelling task {} at {}", taskId, agentUrl);

        String cancelUrl = normalizeUrl(agentUrl) + "/tasks/" + taskId + "/cancel";

        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", taskId,
            "method", "tasks/cancel",
            "params", Map.of("id", taskId)
        );

        return webClient.post()
            .uri(cancelUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(serialize(request))
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(json -> {
                Map<String, Object> response = parse(json);
                return response.containsKey("result") && !response.containsKey("error");
            })
            .onErrorReturn(false)
            .toFuture();
    }

    // ==================== Private Methods ====================

    private String normalizeUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private A2aAgent parseAgentCard(String json, String agentUrl) {
        Map<String, Object> card = parse(json);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skillsData = (List<Map<String, Object>>)
            card.getOrDefault("skills", List.of());

        List<A2aSkill> skills = skillsData.stream()
            .map(s -> A2aSkill.builder()
                .id((String) s.get("id"))
                .name((String) s.get("name"))
                .description((String) s.getOrDefault("description", ""))
                .inputSchema(getMapOrEmpty(s, "inputSchema"))
                .outputSchema(getMapOrEmpty(s, "outputSchema"))
                .build())
            .toList();

        return A2aAgent.builder()
            .id((String) card.getOrDefault("id", agentUrl))
            .name((String) card.getOrDefault("name", "Unknown Agent"))
            .description((String) card.getOrDefault("description", ""))
            .url(agentUrl)
            .provider((String) card.getOrDefault("provider", "unknown"))
            .version((String) card.getOrDefault("version", A2A_VERSION))
            .skills(skills)
            .authentication(getMapOrEmpty(card, "authentication"))
            .metadata(getMapOrEmpty(card, "metadata"))
            .build();
    }

    private A2aTaskResponse parseTaskResponse(String json, String taskId) {
        Map<String, Object> response = parse(json);

        if (response.containsKey("error")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            return new A2aTaskResponse(
                taskId,
                TaskState.FAILED,
                null,
                (String) error.getOrDefault("message", "Unknown error"),
                Map.of()
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getOrDefault("result", Map.of());

        TaskState state = parseTaskState((String) result.getOrDefault("status", "pending"));
        Object output = result.get("output");

        return new A2aTaskResponse(
            taskId,
            state,
            output,
            null,
            getMapOrEmpty(result, "metadata")
        );
    }

    private A2aTaskStatus parseTaskStatus(String json, String taskId) {
        Map<String, Object> response = parse(json);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getOrDefault("result", Map.of());

        TaskState state = parseTaskState((String) result.getOrDefault("status", "pending"));
        double progress = result.containsKey("progress")
            ? ((Number) result.get("progress")).doubleValue()
            : 0.0;
        String message = (String) result.getOrDefault("message", "");

        return new A2aTaskStatus(taskId, state, progress, message, List.of());
    }

    private TaskState parseTaskState(String status) {
        return switch (status.toLowerCase()) {
            case "pending", "submitted" -> TaskState.PENDING;
            case "running", "working", "in_progress" -> TaskState.RUNNING;
            case "completed", "done", "success" -> TaskState.COMPLETED;
            case "failed", "error" -> TaskState.FAILED;
            case "cancelled", "canceled" -> TaskState.CANCELLED;
            default -> TaskState.PENDING;
        };
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new A2aException("Failed to serialize request", e);
        }
    }

    private String serializeInput(Map<String, Object> input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            return input.toString();
        }
    }

    private Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new A2aException("Failed to parse response", e);
        }
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
     * Exception for A2A protocol errors.
     */
    public static class A2aException extends RuntimeException {
        public A2aException(String message) {
            super(message);
        }

        public A2aException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
