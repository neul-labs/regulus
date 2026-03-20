package com.regulus.platform.agents.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating MCP clients with appropriate configuration.
 * Supports both mock and real HTTP clients.
 */
public class McpClientFactory {

    private static final Logger log = LoggerFactory.getLogger(McpClientFactory.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final boolean mockEnabled;
    private final Duration timeout;
    private final WebClient.Builder webClientBuilder;
    private final Map<String, McpClient> clientCache = new ConcurrentHashMap<>();

    public McpClientFactory(boolean mockEnabled, Duration timeout, WebClient.Builder webClientBuilder) {
        this.mockEnabled = mockEnabled;
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
        this.webClientBuilder = webClientBuilder;
        log.info("MCP client factory initialized (mock={}, timeout={})", mockEnabled, this.timeout);
    }

    /**
     * Create a new MCP client for the given server URL.
     * Returns cached client if one already exists for this URL.
     */
    public McpClient createClient(String serverUrl) {
        return clientCache.computeIfAbsent(serverUrl, this::doCreateClient);
    }

    /**
     * Create a new client without caching.
     */
    public McpClient createNewClient(String serverUrl) {
        return doCreateClient(serverUrl);
    }

    private McpClient doCreateClient(String serverUrl) {
        if (mockEnabled) {
            log.info("Creating mock MCP client for {}", serverUrl);
            return new MockMcpClient(serverUrl);
        }

        log.info("Creating HTTP MCP client for {}", serverUrl);

        if (webClientBuilder != null) {
            WebClient webClient = webClientBuilder
                .baseUrl(serverUrl)
                .build();
            return new HttpMcpClient(serverUrl, webClient, timeout);
        }

        return new HttpMcpClient(serverUrl, timeout);
    }

    /**
     * Clear all cached clients.
     */
    public void clearCache() {
        clientCache.clear();
        log.debug("Cleared MCP client cache");
    }

    /**
     * Check if a client exists for the given URL.
     */
    public boolean hasClient(String serverUrl) {
        return clientCache.containsKey(serverUrl);
    }

    /**
     * Remove a client from cache.
     */
    public void removeClient(String serverUrl) {
        clientCache.remove(serverUrl);
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
