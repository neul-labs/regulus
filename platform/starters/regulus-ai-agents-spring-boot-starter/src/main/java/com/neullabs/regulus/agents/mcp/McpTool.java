package com.neullabs.regulus.agents.mcp;

import java.util.Map;

/**
 * Represents an MCP (Model Context Protocol) tool that can be discovered and invoked.
 */
public record McpTool(
    String name,
    String description,
    Map<String, Object> inputSchema,
    String serverUrl
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private Map<String, Object> inputSchema = Map.of();
        private String serverUrl;

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

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public McpTool build() {
            return new McpTool(name, description, inputSchema, serverUrl);
        }
    }
}
