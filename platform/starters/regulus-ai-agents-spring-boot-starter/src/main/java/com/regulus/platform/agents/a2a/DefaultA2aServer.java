package com.regulus.platform.agents.a2a;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default implementation of A2A server.
 * Manages skill registration, task execution, and state tracking.
 */
public class DefaultA2aServer implements A2aServer {

    private static final Logger log = LoggerFactory.getLogger(DefaultA2aServer.class);

    private final A2aAgent agentCard;
    private final Map<String, A2aSkill> skills = new ConcurrentHashMap<>();
    private final Map<String, SkillHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, TaskState> taskStates = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public DefaultA2aServer(A2aAgent agentCard) {
        this.agentCard = agentCard;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("Created A2A server: {} ({})", agentCard.name(), agentCard.id());
    }

    public DefaultA2aServer(A2aAgent agentCard, ExecutorService executor) {
        this.agentCard = agentCard;
        this.executor = executor;
        log.info("Created A2A server: {} ({}) with custom executor", agentCard.name(), agentCard.id());
    }

    @Override
    public A2aAgent getAgentCard() {
        // Return agent card with current skills
        return A2aAgent.builder()
            .id(agentCard.id())
            .name(agentCard.name())
            .description(agentCard.description())
            .url(agentCard.url())
            .provider(agentCard.provider())
            .version(agentCard.version())
            .skills(new ArrayList<>(skills.values()))
            .authentication(agentCard.authentication())
            .metadata(agentCard.metadata())
            .build();
    }

    @Override
    public void registerSkill(A2aSkill skill, SkillHandler handler) {
        skills.put(skill.id(), skill);
        handlers.put(skill.id(), handler);
        log.info("Registered skill: {} - {}", skill.id(), skill.name());
    }

    @Override
    public void unregisterSkill(String skillId) {
        skills.remove(skillId);
        handlers.remove(skillId);
        log.info("Unregistered skill: {}", skillId);
    }

    @Override
    public CompletableFuture<A2aClient.A2aTaskResponse> executeTask(A2aClient.A2aTask task) {
        log.info("Executing task: {} (skill: {})", task.id(), task.skillId());

        // Initialize task state
        TaskState state = new TaskState(
            task.id(),
            task.skillId(),
            A2aClient.TaskState.PENDING,
            0.0,
            "Task queued",
            Instant.now(),
            null
        );
        taskStates.put(task.id(), state);

        return CompletableFuture.supplyAsync(() -> {
            // Update state to running
            updateTaskState(task.id(), A2aClient.TaskState.RUNNING, 0.1, "Task started");

            SkillHandler handler = handlers.get(task.skillId());
            if (handler == null) {
                log.warn("No handler for skill: {}", task.skillId());
                updateTaskState(task.id(), A2aClient.TaskState.FAILED, 0, "Skill not found");
                return new A2aClient.A2aTaskResponse(
                    task.id(),
                    A2aClient.TaskState.FAILED,
                    null,
                    "Skill not found: " + task.skillId(),
                    Map.of()
                );
            }

            try {
                // Execute the skill
                updateTaskState(task.id(), A2aClient.TaskState.RUNNING, 0.5, "Executing skill");
                Object result = handler.execute(task.input(), task.context());

                // Update state to completed
                updateTaskState(task.id(), A2aClient.TaskState.COMPLETED, 1.0, "Task completed");

                log.info("Task {} completed successfully", task.id());
                return new A2aClient.A2aTaskResponse(
                    task.id(),
                    A2aClient.TaskState.COMPLETED,
                    result,
                    null,
                    Map.of("completedAt", Instant.now().toString())
                );

            } catch (Exception e) {
                log.error("Task {} failed: {}", task.id(), e.getMessage(), e);
                updateTaskState(task.id(), A2aClient.TaskState.FAILED, 0, e.getMessage());
                return new A2aClient.A2aTaskResponse(
                    task.id(),
                    A2aClient.TaskState.FAILED,
                    null,
                    e.getMessage(),
                    Map.of()
                );
            }
        }, executor);
    }

    @Override
    public A2aClient.A2aTaskStatus getTaskStatus(String taskId) {
        TaskState state = taskStates.get(taskId);
        if (state == null) {
            return new A2aClient.A2aTaskStatus(
                taskId,
                A2aClient.TaskState.FAILED,
                0,
                "Task not found",
                List.of()
            );
        }

        return new A2aClient.A2aTaskStatus(
            taskId,
            state.state(),
            state.progress(),
            state.message(),
            List.of()
        );
    }

    @Override
    public boolean cancelTask(String taskId) {
        TaskState state = taskStates.get(taskId);
        if (state == null) {
            return false;
        }

        if (state.state() == A2aClient.TaskState.RUNNING ||
            state.state() == A2aClient.TaskState.PENDING) {
            updateTaskState(taskId, A2aClient.TaskState.CANCELLED, state.progress(), "Task cancelled");
            log.info("Task {} cancelled", taskId);
            return true;
        }

        return false;
    }

    private void updateTaskState(String taskId, A2aClient.TaskState state, double progress, String message) {
        TaskState current = taskStates.get(taskId);
        if (current != null) {
            taskStates.put(taskId, new TaskState(
                current.taskId(),
                current.skillId(),
                state,
                progress,
                message,
                current.createdAt(),
                state == A2aClient.TaskState.COMPLETED || state == A2aClient.TaskState.FAILED
                    ? Instant.now() : null
            ));
        }
    }

    /**
     * Get the number of registered skills.
     */
    public int getSkillCount() {
        return skills.size();
    }

    /**
     * Clean up old task states.
     */
    public void cleanupOldTasks(int maxAgeMinutes) {
        Instant cutoff = Instant.now().minusSeconds(maxAgeMinutes * 60L);
        taskStates.entrySet().removeIf(entry ->
            entry.getValue().completedAt() != null &&
            entry.getValue().completedAt().isBefore(cutoff)
        );
    }

    // Internal task state record
    private record TaskState(
        String taskId,
        String skillId,
        A2aClient.TaskState state,
        double progress,
        String message,
        Instant createdAt,
        Instant completedAt
    ) {}
}
