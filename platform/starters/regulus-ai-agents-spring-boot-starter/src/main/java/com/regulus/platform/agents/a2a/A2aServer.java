package com.regulus.platform.agents.a2a;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for A2A server implementations.
 * Exposes the application as an A2A-compatible agent.
 */
public interface A2aServer {

    /**
     * Get the agent card for this server.
     */
    A2aAgent getAgentCard();

    /**
     * Register a skill handler.
     *
     * @param skill the skill definition
     * @param handler the handler to execute the skill
     */
    void registerSkill(A2aSkill skill, SkillHandler handler);

    /**
     * Unregister a skill.
     *
     * @param skillId the ID of the skill to unregister
     */
    void unregisterSkill(String skillId);

    /**
     * Execute a task.
     *
     * @param task the task to execute
     * @return the task response
     */
    CompletableFuture<A2aClient.A2aTaskResponse> executeTask(A2aClient.A2aTask task);

    /**
     * Get the status of a task.
     *
     * @param taskId the ID of the task
     * @return the task status
     */
    A2aClient.A2aTaskStatus getTaskStatus(String taskId);

    /**
     * Cancel a task.
     *
     * @param taskId the ID of the task to cancel
     * @return true if cancellation was successful
     */
    boolean cancelTask(String taskId);

    /**
     * Handler interface for skill execution.
     */
    @FunctionalInterface
    interface SkillHandler {
        Object execute(Map<String, Object> input, Map<String, Object> context) throws Exception;
    }
}
