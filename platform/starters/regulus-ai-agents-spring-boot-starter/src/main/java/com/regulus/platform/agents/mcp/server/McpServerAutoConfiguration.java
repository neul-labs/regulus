package com.regulus.platform.agents.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regulus.platform.agents.autoconfigure.AiAgentsProperties;
import com.regulus.platform.agents.mcp.server.tools.Iso20022ValidatorTool;
import com.regulus.platform.agents.mcp.server.tools.RiskScoringTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for MCP server functionality.
 * Enables the application to act as an MCP server exposing tools.
 */
@AutoConfiguration
@EnableConfigurationProperties(McpServerProperties.class)
@ConditionalOnProperty(name = "regulus.ai.mcp.server.enabled", havingValue = "true")
public class McpServerAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpServerAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public McpServer mcpServer(McpServerProperties properties) {
        DefaultMcpServer server = new DefaultMcpServer(
            properties.getName(),
            properties.getVersion()
        );

        String serverUrl = "http://localhost:" + properties.getPort() + properties.getPath();

        // Register built-in tools
        if (properties.getTools().isIso20022Enabled()) {
            log.info("Registering ISO 20022 validator tool");
            server.registerTool(
                Iso20022ValidatorTool.getToolDefinition(serverUrl),
                new Iso20022ValidatorTool()
            );
        }

        if (properties.getTools().isRiskScoringEnabled()) {
            log.info("Registering risk scoring tool");
            server.registerTool(
                RiskScoringTool.getToolDefinition(serverUrl),
                new RiskScoringTool()
            );
        }

        log.info("MCP server initialized with {} tools", server.getToolCount());
        return server;
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper mcpObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public McpServerController mcpServerController(McpServer mcpServer, ObjectMapper objectMapper) {
        return new McpServerController(mcpServer, objectMapper);
    }
}
