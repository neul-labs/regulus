package com.neullabs.regulus.agents.mcp.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MCP server.
 */
@ConfigurationProperties(prefix = "regulus.ai.mcp.server")
public class McpServerProperties {

    /**
     * Whether MCP server is enabled.
     */
    private boolean enabled = false;

    /**
     * Server name.
     */
    private String name = "regulus-mcp-server";

    /**
     * Server version.
     */
    private String version = "1.0.0";

    /**
     * Server port (inherited from Spring if not set).
     */
    private int port = 8080;

    /**
     * MCP endpoint path.
     */
    private String path = "/mcp";

    /**
     * Tool configuration.
     */
    private ToolConfig tools = new ToolConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ToolConfig getTools() {
        return tools;
    }

    public void setTools(ToolConfig tools) {
        this.tools = tools;
    }

    public static class ToolConfig {
        /**
         * Enable ISO 20022 validator tool.
         */
        private boolean iso20022Enabled = true;

        /**
         * Enable risk scoring tool.
         */
        private boolean riskScoringEnabled = true;

        public boolean isIso20022Enabled() {
            return iso20022Enabled;
        }

        public void setIso20022Enabled(boolean iso20022Enabled) {
            this.iso20022Enabled = iso20022Enabled;
        }

        public boolean isRiskScoringEnabled() {
            return riskScoringEnabled;
        }

        public void setRiskScoringEnabled(boolean riskScoringEnabled) {
            this.riskScoringEnabled = riskScoringEnabled;
        }
    }
}
