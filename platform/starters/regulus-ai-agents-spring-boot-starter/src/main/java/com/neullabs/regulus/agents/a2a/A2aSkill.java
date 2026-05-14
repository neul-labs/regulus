package com.neullabs.regulus.agents.a2a;

import java.util.Map;

/**
 * Represents a skill/capability that an A2A agent can perform.
 */
public record A2aSkill(
    String id,
    String name,
    String description,
    Map<String, Object> inputSchema,
    Map<String, Object> outputSchema,
    Map<String, Object> metadata
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private Map<String, Object> inputSchema = Map.of();
        private Map<String, Object> outputSchema = Map.of();
        private Map<String, Object> metadata = Map.of();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder inputSchema(Map<String, Object> inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        public Builder outputSchema(Map<String, Object> outputSchema) {
            this.outputSchema = outputSchema;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public A2aSkill build() {
            return new A2aSkill(id, name, description, inputSchema, outputSchema, metadata);
        }
    }
}
