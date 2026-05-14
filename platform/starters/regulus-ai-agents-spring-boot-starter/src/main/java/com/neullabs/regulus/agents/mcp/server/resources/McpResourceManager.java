package com.neullabs.regulus.agents.mcp.server.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages MCP resources and resource providers.
 * Aggregates resources from multiple providers and handles subscriptions.
 */
public class McpResourceManager {

    private static final Logger log = LoggerFactory.getLogger(McpResourceManager.class);

    private final List<McpResourceProvider> providers = new CopyOnWriteArrayList<>();
    private final Map<String, List<Consumer<ResourceChangeEvent>>> subscriptions = new ConcurrentHashMap<>();

    /**
     * Register a resource provider.
     */
    public void registerProvider(McpResourceProvider provider) {
        providers.add(provider);
        log.info("Registered resource provider: {}", provider.getProviderName());
    }

    /**
     * Unregister a resource provider.
     */
    public void unregisterProvider(McpResourceProvider provider) {
        providers.remove(provider);
        log.info("Unregistered resource provider: {}", provider.getProviderName());
    }

    /**
     * List all resources from all providers.
     *
     * @param cursor optional pagination cursor
     * @return aggregated list of resources
     */
    public McpResourceProvider.ResourceList listResources(String cursor) {
        List<McpResource> allResources = new ArrayList<>();

        for (McpResourceProvider provider : providers) {
            try {
                McpResourceProvider.ResourceList list = provider.listResources(cursor);
                allResources.addAll(list.resources());
            } catch (Exception e) {
                log.error("Error listing resources from provider {}: {}",
                    provider.getProviderName(), e.getMessage());
            }
        }

        log.debug("Listed {} total resources from {} providers", allResources.size(), providers.size());
        return McpResourceProvider.ResourceList.of(allResources);
    }

    /**
     * Read a resource by URI.
     * Finds the appropriate provider and reads the resource.
     *
     * @param uri the resource URI
     * @return the resource content if found
     */
    public Optional<McpResourceProvider.ResourceContent> readResource(String uri) {
        for (McpResourceProvider provider : providers) {
            if (provider.canHandle(uri)) {
                try {
                    Optional<McpResourceProvider.ResourceContent> content = provider.readResource(uri);
                    if (content.isPresent()) {
                        log.debug("Read resource '{}' from provider '{}'", uri, provider.getProviderName());
                        return content;
                    }
                } catch (Exception e) {
                    log.error("Error reading resource '{}' from provider {}: {}",
                        uri, provider.getProviderName(), e.getMessage());
                }
            }
        }

        log.warn("Resource not found: {}", uri);
        return Optional.empty();
    }

    /**
     * Subscribe to changes on a resource.
     *
     * @param uri the resource URI
     * @param listener the change listener
     * @return true if subscription was successful
     */
    public boolean subscribe(String uri, Consumer<ResourceChangeEvent> listener) {
        boolean subscribed = false;

        for (McpResourceProvider provider : providers) {
            if (provider.canHandle(uri)) {
                if (provider.subscribe(uri)) {
                    subscriptions.computeIfAbsent(uri, k -> new CopyOnWriteArrayList<>()).add(listener);
                    subscribed = true;
                    log.debug("Subscribed to resource changes: {}", uri);
                    break;
                }
            }
        }

        return subscribed;
    }

    /**
     * Unsubscribe from resource changes.
     *
     * @param uri the resource URI
     * @param listener the listener to remove
     */
    public void unsubscribe(String uri, Consumer<ResourceChangeEvent> listener) {
        List<Consumer<ResourceChangeEvent>> listeners = subscriptions.get(uri);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                subscriptions.remove(uri);
                providers.stream()
                    .filter(p -> p.canHandle(uri))
                    .findFirst()
                    .ifPresent(p -> p.unsubscribe(uri));
            }
        }
        log.debug("Unsubscribed from resource changes: {}", uri);
    }

    /**
     * Notify subscribers of a resource change.
     * Called by resource providers when content changes.
     *
     * @param event the change event
     */
    public void notifyChange(ResourceChangeEvent event) {
        List<Consumer<ResourceChangeEvent>> listeners = subscriptions.get(event.uri());
        if (listeners != null) {
            for (Consumer<ResourceChangeEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    log.error("Error notifying resource change listener: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Get the number of registered providers.
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Get the total number of active subscriptions.
     */
    public int getSubscriptionCount() {
        return subscriptions.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    /**
     * Resource change event.
     */
    public record ResourceChangeEvent(
        String uri,
        ChangeType type
    ) {
        public enum ChangeType {
            CREATED,
            UPDATED,
            DELETED
        }

        public static ResourceChangeEvent created(String uri) {
            return new ResourceChangeEvent(uri, ChangeType.CREATED);
        }

        public static ResourceChangeEvent updated(String uri) {
            return new ResourceChangeEvent(uri, ChangeType.UPDATED);
        }

        public static ResourceChangeEvent deleted(String uri) {
            return new ResourceChangeEvent(uri, ChangeType.DELETED);
        }
    }
}
