package com.neullabs.regulus.llm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request to an LLM provider.
 */
public record LlmRequest(
    String id,
    String prompt,
    List<Message> messages,
    String systemPrompt,
    Map<String, Object> parameters,
    List<Tool> tools,
    LlmOptions options,
    Map<String, String> metadata
) {
    public LlmRequest {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (parameters == null) {
            parameters = Map.of();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Chat message with role and content.
     */
    public record Message(Role role, String content) {
        public enum Role {
            SYSTEM, USER, ASSISTANT, TOOL
        }
    }

    /**
     * Tool definition for function calling.
     */
    public record Tool(
        String name,
        String description,
        Map<String, Object> inputSchema
    ) {}

    /**
     * LLM generation options.
     */
    public record LlmOptions(
        Double temperature,
        Integer maxTokens,
        Double topP,
        Integer topK,
        List<String> stopSequences,
        Boolean stream
    ) {
        public static LlmOptions defaults() {
            return new LlmOptions(0.7, 4096, 0.95, 40, List.of(), false);
        }
    }

    public static class Builder {
        private String id;
        private String prompt;
        private List<Message> messages;
        private String systemPrompt;
        private Map<String, Object> parameters = Map.of();
        private List<Tool> tools;
        private LlmOptions options = LlmOptions.defaults();
        private Map<String, String> metadata = Map.of();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder tools(List<Tool> tools) {
            this.tools = tools;
            return this;
        }

        public Builder options(LlmOptions options) {
            this.options = options;
            return this;
        }

        public Builder temperature(double temperature) {
            this.options = new LlmOptions(
                temperature,
                this.options.maxTokens(),
                this.options.topP(),
                this.options.topK(),
                this.options.stopSequences(),
                this.options.stream()
            );
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.options = new LlmOptions(
                this.options.temperature(),
                maxTokens,
                this.options.topP(),
                this.options.topK(),
                this.options.stopSequences(),
                this.options.stream()
            );
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public LlmRequest build() {
            return new LlmRequest(id, prompt, messages, systemPrompt, parameters, tools, options, metadata);
        }
    }
}
