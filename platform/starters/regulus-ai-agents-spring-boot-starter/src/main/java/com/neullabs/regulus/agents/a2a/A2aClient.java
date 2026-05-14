package com.neullabs.regulus.agents.a2a;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Client interface for A2A (Agent-to-Agent) communication.
 * Enables agents to discover and invoke other agents' capabilities.
 */
public interface A2aClient {

    /**
     * Discover the agent card (metadata) from a remote agent.
     *
     * @param agentUrl the URL of the remote agent
     * @return the agent's card/metadata
     */
    CompletableFuture<A2aAgent> discoverAgent(String agentUrl);

    /**
     * Send a task request to a remote agent.
     *
     * @param agentUrl the URL of the remote agent
     * @param task the task to execute
     * @return the task response
     */
    CompletableFuture<A2aTaskResponse> sendTask(String agentUrl, A2aTask task);

    /**
     * Get the status of a running task.
     *
     * @param agentUrl the URL of the remote agent
     * @param taskId the ID of the task
     * @return the current task status
     */
    CompletableFuture<A2aTaskStatus> getTaskStatus(String agentUrl, String taskId);

    /**
     * Cancel a running task.
     *
     * @param agentUrl the URL of the remote agent
     * @param taskId the ID of the task to cancel
     * @return true if cancellation was successful
     */
    CompletableFuture<Boolean> cancelTask(String agentUrl, String taskId);

    /**
     * A2A task request.
     */
    record A2aTask(
        String id,
        String skillId,
        Map<String, Object> input,
        Map<String, Object> context,
        Map<String, Object> metadata
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String id;
            private String skillId;
            private Map<String, Object> input = Map.of();
            private Map<String, Object> context = Map.of();
            private Map<String, Object> metadata = Map.of();

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder skillId(String skillId) {
                this.skillId = skillId;
                return this;
            }

            public Builder input(Map<String, Object> input) {
                this.input = input;
                return this;
            }

            public Builder context(Map<String, Object> context) {
                this.context = context;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public A2aTask build() {
                if (id == null) {
                    id = java.util.UUID.randomUUID().toString();
                }
                return new A2aTask(id, skillId, input, context, metadata);
            }
        }
    }

    /**
     * A2A task response.
     */
    record A2aTaskResponse(
        String taskId,
        TaskState state,
        Object result,
        String error,
        Map<String, Object> metadata
    ) {
        public boolean isSuccess() {
            return state == TaskState.COMPLETED && error == null;
        }

        public boolean isFailed() {
            return state == TaskState.FAILED || error != null;
        }

        public boolean isPending() {
            return state == TaskState.PENDING || state == TaskState.RUNNING;
        }
    }

    /**
     * A2A task status.
     */
    record A2aTaskStatus(
        String taskId,
        TaskState state,
        double progress,
        String message,
        List<A2aArtifact> artifacts
    ) {}

    /**
     * Artifact produced during task execution.
     */
    record A2aArtifact(
        String name,
        String type,
        Object content,
        Map<String, Object> metadata
    ) {}

    /**
     * Task execution states.
     */
    enum TaskState {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
