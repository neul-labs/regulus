package com.regulus.platform.agents.mcp.server.resources;

import java.util.List;
import java.util.Optional;

/**
 * Interface for providing MCP resources.
 * Implementations can provide resources from various sources like files, databases, APIs, etc.
 */
public interface McpResourceProvider {

    /**
     * List all available resources.
     * May optionally filter by a cursor for pagination.
     *
     * @param cursor optional pagination cursor
     * @return list of available resources
     */
    ResourceList listResources(String cursor);

    /**
     * Read a specific resource by URI.
     *
     * @param uri the resource URI
     * @return the resource content if found
     */
    Optional<ResourceContent> readResource(String uri);

    /**
     * Subscribe to resource changes.
     * Returns true if subscriptions are supported.
     *
     * @param uri the resource URI to subscribe to
     * @return true if subscription was successful
     */
    default boolean subscribe(String uri) {
        return false;
    }

    /**
     * Unsubscribe from resource changes.
     *
     * @param uri the resource URI to unsubscribe from
     */
    default void unsubscribe(String uri) {
    }

    /**
     * Get the provider name for identification.
     */
    String getProviderName();

    /**
     * Check if this provider can handle a given URI scheme.
     */
    boolean canHandle(String uri);

    /**
     * List of resources with optional pagination cursor.
     */
    record ResourceList(
        List<McpResource> resources,
        String nextCursor
    ) {
        public static ResourceList of(List<McpResource> resources) {
            return new ResourceList(resources, null);
        }

        public static ResourceList of(List<McpResource> resources, String nextCursor) {
            return new ResourceList(resources, nextCursor);
        }
    }

    /**
     * Resource content with metadata.
     */
    record ResourceContent(
        String uri,
        String mimeType,
        String text,
        byte[] blob
    ) {
        public static ResourceContent text(String uri, String content) {
            return new ResourceContent(uri, "text/plain", content, null);
        }

        public static ResourceContent text(String uri, String mimeType, String content) {
            return new ResourceContent(uri, mimeType, content, null);
        }

        public static ResourceContent json(String uri, String content) {
            return new ResourceContent(uri, "application/json", content, null);
        }

        public static ResourceContent blob(String uri, String mimeType, byte[] data) {
            return new ResourceContent(uri, mimeType, null, data);
        }
    }
}
