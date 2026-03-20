package com.regulus.platform.agents.mcp.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regulus.platform.agents.mcp.McpTool;
import com.regulus.platform.agents.mcp.server.prompts.McpPrompt;
import com.regulus.platform.agents.mcp.server.prompts.McpPromptManager;
import com.regulus.platform.agents.mcp.server.resources.McpResource;
import com.regulus.platform.agents.mcp.server.resources.McpResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller exposing MCP server endpoints.
 * Implements the MCP protocol over HTTP/JSON-RPC.
 */
@RestController
@RequestMapping("${regulus.ai.mcp.server.path:/mcp}")
public class McpServerController {

    private static final Logger log = LoggerFactory.getLogger(McpServerController.class);
    private static final String JSONRPC_VERSION = "2.0";

    private final McpServer mcpServer;
    private final ObjectMapper objectMapper;

    public McpServerController(McpServer mcpServer, ObjectMapper objectMapper) {
        this.mcpServer = mcpServer;
        this.objectMapper = objectMapper;
        log.info("MCP server controller initialized");
    }

    /**
     * Handle JSON-RPC requests.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> handleRequest(@RequestBody Map<String, Object> request) {
        String id = request.get("id") != null ? String.valueOf(request.get("id")) : null;
        String method = (String) request.get("method");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());

        log.debug("Received MCP request: method={}, id={}", method, id);

        return switch (method) {
            case "initialize" -> handleInitialize(id, params);
            case "initialized" -> handleInitialized(id);
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolsCall(id, params);
            case "resources/list" -> handleResourcesList(id, params);
            case "resources/read" -> handleResourcesRead(id, params);
            case "resources/subscribe" -> handleResourcesSubscribe(id, params);
            case "resources/unsubscribe" -> handleResourcesUnsubscribe(id, params);
            case "prompts/list" -> handlePromptsList(id, params);
            case "prompts/get" -> handlePromptsGet(id, params);
            case "ping" -> handlePing(id);
            default -> Mono.just(errorResponse(id, -32601, "Method not found: " + method));
        };
    }

    private Mono<Map<String, Object>> handleInitialize(String id, Map<String, Object> params) {
        McpServer.ServerInfo info = mcpServer.getServerInfo();

        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", info.capabilities());
        result.put("serverInfo", Map.of(
            "name", info.name(),
            "version", info.version()
        ));

        log.info("Client initialized: {}", params.get("clientInfo"));
        return Mono.just(successResponse(id, result));
    }

    private Mono<Map<String, Object>> handleInitialized(String id) {
        log.debug("Client sent initialized notification");
        // This is a notification, no response needed but we send an empty success for HTTP
        if (id == null) {
            return Mono.empty();
        }
        return Mono.just(successResponse(id, Map.of()));
    }

    private Mono<Map<String, Object>> handleToolsList(String id) {
        List<McpTool> tools = mcpServer.getTools();

        List<Map<String, Object>> toolDefs = tools.stream()
            .map(tool -> {
                Map<String, Object> toolDef = new HashMap<>();
                toolDef.put("name", tool.name());
                toolDef.put("description", tool.description());
                toolDef.put("inputSchema", tool.inputSchema());
                return toolDef;
            })
            .toList();

        log.debug("Returning {} tools", tools.size());
        return Mono.just(successResponse(id, Map.of("tools", toolDefs)));
    }

    private Mono<Map<String, Object>> handleToolsCall(String id, Map<String, Object> params) {
        String toolName = (String) params.get("name");

        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

        log.debug("Calling tool '{}' with arguments: {}", toolName, arguments);

        return Mono.fromFuture(mcpServer.executeTool(toolName, arguments))
            .map(result -> {
                List<Map<String, Object>> content = result.content().stream()
                    .map(item -> {
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
                    })
                    .toList();

                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("content", content);
                resultMap.put("isError", result.isError());

                return successResponse(id, resultMap);
            })
            .onErrorResume(e -> {
                log.error("Tool execution error: {}", e.getMessage());
                return Mono.just(errorResponse(id, -32000, e.getMessage()));
            });
    }

    private Mono<Map<String, Object>> handlePing(String id) {
        return Mono.just(successResponse(id, Map.of()));
    }

    // ============ Resources Handlers ============

    private Mono<Map<String, Object>> handleResourcesList(String id, Map<String, Object> params) {
        String cursor = (String) params.get("cursor");

        McpResourceProvider.ResourceList resourceList = mcpServer.listResources(cursor);

        List<Map<String, Object>> resources = resourceList.resources().stream()
            .map(resource -> {
                Map<String, Object> resourceDef = new HashMap<>();
                resourceDef.put("uri", resource.uri());
                resourceDef.put("name", resource.name());
                resourceDef.put("description", resource.description());
                resourceDef.put("mimeType", resource.mimeType());
                if (resource.annotations() != null && !resource.annotations().isEmpty()) {
                    resourceDef.put("annotations", resource.annotations());
                }
                return resourceDef;
            })
            .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("resources", resources);
        if (resourceList.nextCursor() != null) {
            result.put("nextCursor", resourceList.nextCursor());
        }

        log.debug("Returning {} resources", resources.size());
        return Mono.just(successResponse(id, result));
    }

    private Mono<Map<String, Object>> handleResourcesRead(String id, Map<String, Object> params) {
        String uri = (String) params.get("uri");

        if (uri == null || uri.isBlank()) {
            return Mono.just(errorResponse(id, -32602, "Missing required parameter: uri"));
        }

        Optional<McpResourceProvider.ResourceContent> contentOpt = mcpServer.readResource(uri);

        if (contentOpt.isEmpty()) {
            return Mono.just(errorResponse(id, -32002, "Resource not found: " + uri));
        }

        McpResourceProvider.ResourceContent content = contentOpt.get();

        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("uri", content.uri());
        contentItem.put("mimeType", content.mimeType());

        if (content.text() != null) {
            contentItem.put("text", content.text());
        } else if (content.blob() != null) {
            contentItem.put("blob", Base64.getEncoder().encodeToString(content.blob()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("contents", List.of(contentItem));

        log.debug("Read resource: {}", uri);
        return Mono.just(successResponse(id, result));
    }

    private Mono<Map<String, Object>> handleResourcesSubscribe(String id, Map<String, Object> params) {
        String uri = (String) params.get("uri");

        if (uri == null || uri.isBlank()) {
            return Mono.just(errorResponse(id, -32602, "Missing required parameter: uri"));
        }

        // Note: subscription callbacks would require SSE endpoint for notifications
        log.debug("Subscribed to resource: {}", uri);
        return Mono.just(successResponse(id, Map.of()));
    }

    private Mono<Map<String, Object>> handleResourcesUnsubscribe(String id, Map<String, Object> params) {
        String uri = (String) params.get("uri");

        if (uri == null || uri.isBlank()) {
            return Mono.just(errorResponse(id, -32602, "Missing required parameter: uri"));
        }

        log.debug("Unsubscribed from resource: {}", uri);
        return Mono.just(successResponse(id, Map.of()));
    }

    // ============ Prompts Handlers ============

    private Mono<Map<String, Object>> handlePromptsList(String id, Map<String, Object> params) {
        String cursor = (String) params.get("cursor");

        McpPromptManager.PromptList promptList = mcpServer.listPrompts(cursor);

        List<Map<String, Object>> prompts = promptList.prompts().stream()
            .map(prompt -> {
                Map<String, Object> promptDef = new HashMap<>();
                promptDef.put("name", prompt.name());
                promptDef.put("description", prompt.description());

                if (prompt.arguments() != null && !prompt.arguments().isEmpty()) {
                    List<Map<String, Object>> args = prompt.arguments().stream()
                        .map(arg -> Map.<String, Object>of(
                            "name", arg.name(),
                            "description", arg.description(),
                            "required", arg.required()
                        ))
                        .toList();
                    promptDef.put("arguments", args);
                }

                return promptDef;
            })
            .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("prompts", prompts);
        if (promptList.nextCursor() != null) {
            result.put("nextCursor", promptList.nextCursor());
        }

        log.debug("Returning {} prompts", prompts.size());
        return Mono.just(successResponse(id, result));
    }

    private Mono<Map<String, Object>> handlePromptsGet(String id, Map<String, Object> params) {
        String name = (String) params.get("name");

        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

        if (name == null || name.isBlank()) {
            return Mono.just(errorResponse(id, -32602, "Missing required parameter: name"));
        }

        Optional<McpPromptManager.PromptResult> resultOpt = mcpServer.executePrompt(name, arguments);

        if (resultOpt.isEmpty()) {
            return Mono.just(errorResponse(id, -32002, "Prompt not found: " + name));
        }

        McpPromptManager.PromptResult promptResult = resultOpt.get();

        if (promptResult.isError()) {
            return Mono.just(errorResponse(id, -32000, promptResult.description()));
        }

        List<Map<String, Object>> messages = promptResult.messages().stream()
            .map(msg -> {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("role", msg.role());

                McpPromptManager.MessageContent content = msg.content();
                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("type", content.type());

                if ("text".equals(content.type())) {
                    contentMap.put("text", content.data());
                } else if ("image".equals(content.type())) {
                    contentMap.putAll(asMap(content.data()));
                } else if ("resource".equals(content.type())) {
                    contentMap.putAll(asMap(content.data()));
                }

                msgMap.put("content", contentMap);
                return msgMap;
            })
            .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("description", promptResult.description());
        result.put("messages", messages);

        log.debug("Executed prompt '{}' with {} messages", name, messages.size());
        return Mono.just(successResponse(id, result));
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Map.of("data", obj);
    }
}
