package com.regulus.platform.agents.a2a;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regulus.platform.agents.mcp.McpTool;
import com.regulus.platform.agents.mcp.server.McpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Auto-configuration for A2A (Agent-to-Agent) functionality.
 * Enables the application to act as an A2A agent and communicate with other agents.
 */
@AutoConfiguration
@EnableConfigurationProperties(A2aProperties.class)
@ConditionalOnProperty(name = "regulus.ai.a2a.enabled", havingValue = "true")
public class A2aAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(A2aAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public A2aClient a2aClient(A2aProperties properties, WebClient.Builder webClientBuilder) {
        Duration timeout = Duration.ofMillis(properties.getRequestTimeout());

        log.info("Creating HTTP A2A client (timeout={}ms)", properties.getRequestTimeout());

        WebClient webClient = webClientBuilder.build();
        return new HttpA2aClient(webClient, timeout);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.a2a.server.enabled", havingValue = "true")
    public A2aServer a2aServer(A2aProperties properties) {
        A2aProperties.ServerConfig serverConfig = properties.getServer();

        String baseUrl = "http://localhost:" + serverConfig.getPort();

        A2aAgent agentCard = A2aAgent.builder()
            .id(serverConfig.getId())
            .name(serverConfig.getName())
            .description(serverConfig.getDescription())
            .url(baseUrl)
            .provider(serverConfig.getProvider())
            .version(serverConfig.getVersion())
            .authentication(Map.of(
                "schemes", List.of("bearer")
            ))
            .build();

        DefaultA2aServer server = new DefaultA2aServer(agentCard);

        log.info("Created A2A server: {} ({})", serverConfig.getName(), serverConfig.getId());
        return server;
    }

    /**
     * Optionally bridge MCP tools to A2A skills.
     * This allows MCP tools to be exposed as A2A skills.
     */
    @Bean
    @ConditionalOnBean({A2aServer.class, McpServer.class})
    @ConditionalOnProperty(name = "regulus.ai.a2a.bridge-mcp", havingValue = "true")
    public McpToA2aBridge mcpToA2aBridge(A2aServer a2aServer, McpServer mcpServer) {
        log.info("Bridging MCP tools to A2A skills");

        // Convert each MCP tool to an A2A skill
        for (McpTool tool : mcpServer.getTools()) {
            A2aSkill skill = A2aSkill.builder()
                .id("mcp:" + tool.name())
                .name(tool.name())
                .description(tool.description())
                .inputSchema(tool.inputSchema())
                .build();

            a2aServer.registerSkill(skill, (input, context) -> {
                var response = mcpServer.executeTool(tool.name(), input).join();
                if (response.isError()) {
                    throw new RuntimeException("MCP tool error: " + response.content());
                }
                return response.content().get(0).data();
            });

            log.debug("Bridged MCP tool '{}' to A2A skill 'mcp:{}'", tool.name(), tool.name());
        }

        return new McpToA2aBridge(mcpServer, a2aServer);
    }

    @Bean
    @ConditionalOnBean(A2aServer.class)
    @ConditionalOnMissingBean
    public A2aServerController a2aServerController(A2aServer a2aServer, ObjectMapper objectMapper) {
        return new A2aServerController(a2aServer, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper a2aObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * Bridge class that connects MCP tools to A2A skills.
     */
    public static class McpToA2aBridge {
        private final McpServer mcpServer;
        private final A2aServer a2aServer;

        public McpToA2aBridge(McpServer mcpServer, A2aServer a2aServer) {
            this.mcpServer = mcpServer;
            this.a2aServer = a2aServer;
        }

        public McpServer getMcpServer() {
            return mcpServer;
        }

        public A2aServer getA2aServer() {
            return a2aServer;
        }
    }
}
