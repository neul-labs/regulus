package com.neullabs.regulus.agents.mcp.server.prompts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages MCP prompt templates.
 * Provides registration, listing, and execution of prompts.
 */
public class McpPromptManager {

    private static final Logger log = LoggerFactory.getLogger(McpPromptManager.class);

    private final Map<String, McpPrompt> prompts = new ConcurrentHashMap<>();
    private final Map<String, PromptHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Register a prompt with its handler.
     *
     * @param prompt the prompt definition
     * @param handler the handler to generate prompt messages
     */
    public void registerPrompt(McpPrompt prompt, PromptHandler handler) {
        prompts.put(prompt.name(), prompt);
        handlers.put(prompt.name(), handler);
        log.info("Registered prompt: {} - {}", prompt.name(), prompt.description());
    }

    /**
     * Unregister a prompt.
     *
     * @param name the prompt name
     */
    public void unregisterPrompt(String name) {
        prompts.remove(name);
        handlers.remove(name);
        log.info("Unregistered prompt: {}", name);
    }

    /**
     * List all registered prompts.
     *
     * @param cursor optional pagination cursor
     * @return list of prompts
     */
    public PromptList listPrompts(String cursor) {
        return PromptList.of(new ArrayList<>(prompts.values()));
    }

    /**
     * Get a prompt by name.
     *
     * @param name the prompt name
     * @return the prompt if found
     */
    public Optional<McpPrompt> getPrompt(String name) {
        return Optional.ofNullable(prompts.get(name));
    }

    /**
     * Execute a prompt with the given arguments.
     *
     * @param name the prompt name
     * @param arguments the arguments to pass to the prompt
     * @return the generated messages
     */
    public Optional<PromptResult> executePrompt(String name, Map<String, Object> arguments) {
        McpPrompt prompt = prompts.get(name);
        PromptHandler handler = handlers.get(name);

        if (prompt == null || handler == null) {
            log.warn("Prompt '{}' not found", name);
            return Optional.empty();
        }

        // Validate required arguments
        for (McpPrompt.PromptArgument arg : prompt.arguments()) {
            if (arg.required() && !arguments.containsKey(arg.name())) {
                log.warn("Missing required argument '{}' for prompt '{}'", arg.name(), name);
                return Optional.of(PromptResult.error("Missing required argument: " + arg.name()));
            }
        }

        try {
            log.debug("Executing prompt '{}' with arguments: {}", name, arguments);
            PromptResult result = handler.handle(arguments);
            log.debug("Prompt '{}' executed successfully", name);
            return Optional.of(result);
        } catch (Exception e) {
            log.error("Error executing prompt '{}': {}", name, e.getMessage(), e);
            return Optional.of(PromptResult.error("Prompt execution failed: " + e.getMessage()));
        }
    }

    /**
     * Get the number of registered prompts.
     */
    public int getPromptCount() {
        return prompts.size();
    }

    /**
     * List of prompts with optional pagination cursor.
     */
    public record PromptList(
        List<McpPrompt> prompts,
        String nextCursor
    ) {
        public static PromptList of(List<McpPrompt> prompts) {
            return new PromptList(prompts, null);
        }

        public static PromptList of(List<McpPrompt> prompts, String nextCursor) {
            return new PromptList(prompts, nextCursor);
        }
    }

    /**
     * Result of prompt execution.
     */
    public record PromptResult(
        String description,
        List<PromptMessage> messages,
        boolean isError
    ) {
        public static PromptResult success(String description, List<PromptMessage> messages) {
            return new PromptResult(description, messages, false);
        }

        public static PromptResult error(String message) {
            return new PromptResult(message, List.of(), true);
        }
    }

    /**
     * A message in a prompt result.
     */
    public record PromptMessage(
        String role,
        MessageContent content
    ) {
        public static PromptMessage user(String text) {
            return new PromptMessage("user", MessageContent.text(text));
        }

        public static PromptMessage assistant(String text) {
            return new PromptMessage("assistant", MessageContent.text(text));
        }

        public static PromptMessage system(String text) {
            return new PromptMessage("system", MessageContent.text(text));
        }
    }

    /**
     * Content of a prompt message.
     */
    public record MessageContent(
        String type,
        Object data
    ) {
        public static MessageContent text(String text) {
            return new MessageContent("text", text);
        }

        public static MessageContent image(String base64, String mimeType) {
            return new MessageContent("image", Map.of("data", base64, "mimeType", mimeType));
        }

        public static MessageContent resource(String uri, String mimeType, String text) {
            return new MessageContent("resource", Map.of("uri", uri, "mimeType", mimeType, "text", text));
        }
    }

    /**
     * Handler interface for prompt execution.
     */
    @FunctionalInterface
    public interface PromptHandler {
        PromptResult handle(Map<String, Object> arguments) throws Exception;
    }
}
