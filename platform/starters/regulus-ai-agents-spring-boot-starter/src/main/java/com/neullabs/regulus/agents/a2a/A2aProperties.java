package com.neullabs.regulus.agents.a2a;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

/**
 * Configuration properties for A2A (Agent-to-Agent) functionality.
 */
@ConfigurationProperties(prefix = "regulus.ai.a2a")
public class A2aProperties {

    /**
     * Whether A2A is enabled.
     */
    private boolean enabled = false;

    /**
     * Request timeout in milliseconds.
     */
    private int requestTimeout = 30000;

    /**
     * Whether to bridge MCP tools to A2A skills.
     */
    private boolean bridgeMcp = false;

    /**
     * A2A server configuration.
     */
    private ServerConfig server = new ServerConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public boolean isBridgeMcp() {
        return bridgeMcp;
    }

    public void setBridgeMcp(boolean bridgeMcp) {
        this.bridgeMcp = bridgeMcp;
    }

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }

    public static class ServerConfig {
        /**
         * Whether to enable A2A server mode.
         */
        private boolean enabled = false;

        /**
         * Unique agent ID.
         */
        private String id = "regulus-agent-" + UUID.randomUUID().toString().substring(0, 8);

        /**
         * Agent name.
         */
        private String name = "Regulus AI Agent";

        /**
         * Agent description.
         */
        private String description = "Regulus AI Agent for financial services";

        /**
         * Agent provider/organization.
         */
        private String provider = "Regulus";

        /**
         * Agent version.
         */
        private String version = "1.0.0";

        /**
         * Server port.
         */
        private int port = 8080;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
