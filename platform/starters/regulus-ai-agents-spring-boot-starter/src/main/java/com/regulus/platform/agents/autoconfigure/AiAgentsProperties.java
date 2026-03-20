package com.regulus.platform.agents.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AI Agents starter.
 */
@ConfigurationProperties(prefix = "regulus.ai")
public class AiAgentsProperties {

    /**
     * Model configuration.
     */
    private ModelConfig model = new ModelConfig();

    /**
     * MCP (Model Context Protocol) configuration.
     */
    private McpConfig mcp = new McpConfig();

    /**
     * Agent configuration.
     */
    private AgentConfig agent = new AgentConfig();

    public ModelConfig getModel() {
        return model;
    }

    public void setModel(ModelConfig model) {
        this.model = model;
    }

    public McpConfig getMcp() {
        return mcp;
    }

    public void setMcp(McpConfig mcp) {
        this.mcp = mcp;
    }

    public AgentConfig getAgent() {
        return agent;
    }

    public void setAgent(AgentConfig agent) {
        this.agent = agent;
    }

    public static class ModelConfig {
        /**
         * Default model provider (openai, azure-openai, ollama).
         */
        private String provider = "openai";

        /**
         * Default model name.
         */
        private String name = "gpt-4o";

        /**
         * API key for the model provider.
         */
        private String apiKey;

        /**
         * Base URL for the model API (optional).
         */
        private String baseUrl;

        /**
         * Temperature for model inference.
         */
        private double temperature = 0.7;

        /**
         * Maximum tokens for model response.
         */
        private int maxTokens = 4096;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class McpConfig {
        /**
         * Whether MCP is enabled.
         */
        private boolean enabled = true;

        /**
         * Whether to use mock MCP client (for development/testing).
         */
        private boolean mock = false;

        /**
         * MCP server URL.
         */
        private String serverUrl = "http://localhost:8080/mcp";

        /**
         * Whether to auto-connect on startup.
         */
        private boolean autoConnect = true;

        /**
         * Connection timeout in milliseconds.
         */
        private int connectionTimeout = 5000;

        /**
         * Request timeout in milliseconds.
         */
        private int requestTimeout = 30000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public boolean isAutoConnect() {
            return autoConnect;
        }

        public void setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public boolean isMock() {
            return mock;
        }

        public void setMock(boolean mock) {
            this.mock = mock;
        }

        public int getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(int requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    public static class AgentConfig {
        /**
         * Default agent timeout in seconds.
         */
        private int timeoutSeconds = 60;

        /**
         * Maximum iterations for agent loops.
         */
        private int maxIterations = 10;

        /**
         * Whether to enable verbose logging.
         */
        private boolean verbose = false;

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxIterations() {
            return maxIterations;
        }

        public void setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
        }

        public boolean isVerbose() {
            return verbose;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }
    }
}
