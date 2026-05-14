package com.neullabs.regulus.agents.autoconfigure;

import com.neullabs.regulus.agents.mcp.McpClient;
import com.neullabs.regulus.agents.mcp.McpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Auto-configuration for AI Agents starter.
 * Provides MCP client, LangChain4j integration, and agent infrastructure.
 */
@AutoConfiguration
@EnableConfigurationProperties(AiAgentsProperties.class)
public class AiAgentsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiAgentsAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder mcpWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.mcp.enabled", havingValue = "true", matchIfMissing = true)
    public McpClientFactory mcpClientFactory(AiAgentsProperties properties, WebClient.Builder webClientBuilder) {
        AiAgentsProperties.McpConfig mcpConfig = properties.getMcp();

        Duration timeout = Duration.ofMillis(mcpConfig.getRequestTimeout());

        log.info("Creating MCP client factory (mock={}, timeout={}ms)",
            mcpConfig.isMock(), mcpConfig.getRequestTimeout());

        return new McpClientFactory(mcpConfig.isMock(), timeout, webClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.mcp.enabled", havingValue = "true", matchIfMissing = true)
    public McpClient mcpClient(McpClientFactory factory, AiAgentsProperties properties) {
        AiAgentsProperties.McpConfig mcpConfig = properties.getMcp();
        String serverUrl = mcpConfig.getServerUrl();

        log.info("Creating MCP client for server: {} (mock={})", serverUrl, mcpConfig.isMock());

        McpClient client = factory.createClient(serverUrl);

        // Auto-connect if configured
        if (mcpConfig.isAutoConnect()) {
            log.info("Auto-connecting MCP client to {}", serverUrl);
            try {
                client.connect().join();
                log.info("MCP client connected successfully");
            } catch (Exception e) {
                log.warn("Failed to auto-connect MCP client: {}", e.getMessage());
                // Don't fail startup, allow retry later
            }
        }

        return client;
    }
}
