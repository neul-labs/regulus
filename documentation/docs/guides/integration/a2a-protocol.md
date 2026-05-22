# A2A Protocol

Agent-to-Agent (A2A) protocol for cross-agent communication and task coordination.

!!! note "Cross-org calls are signed"
    For cross-organisation deployments where audit linking matters, Regulus
    signs outbound A2A envelopes with RFC 9421 HTTP Message Signatures and
    verifies inbound ones via the same SPI. See
    [Security architecture → A2A request signing](../../advanced/security-architecture.md#a2a-request-signing)
    for the signature base, replay protection, and key-rotation surface.

## Overview

The A2A protocol enables AI agents to:

- Discover other agents and their capabilities
- Delegate tasks to specialized agents
- Stream progress updates
- Coordinate complex multi-agent workflows

## Configuration

### Basic Setup

```yaml title="application.yml"
regulus:
  a2a:
    enabled: true
    agent:
      id: customer-support-agent
      name: Customer Support Agent
      description: Handles customer inquiries and support requests
      capabilities:
        - customer-lookup
        - account-inquiry
        - complaint-handling
```

### Full Configuration

```yaml title="application.yml"
regulus:
  a2a:
    enabled: true
    agent:
      id: customer-support-agent
      name: Customer Support Agent
      description: Handles customer inquiries and support requests
      version: "1.0.0"
      capabilities:
        - customer-lookup
        - account-inquiry
        - complaint-handling
      skills:
        - name: answer-query
          description: Answer customer questions
          input-schema:
            type: object
            properties:
              query:
                type: string
              customerId:
                type: string
            required: [query]

    # Discovery
    discovery:
      enabled: true
      registry: consul  # consul, eureka, or static
      consul:
        host: localhost
        port: 8500

    # Server settings
    server:
      port: 8082
      path: /a2a

    # Client settings
    client:
      timeout: 30s
      retry:
        max-attempts: 3
```

## Agent Card

Every A2A agent publishes an agent card describing its capabilities:

```java
@Component
public class AgentCardProvider {

    @Bean
    public AgentCard agentCard(A2aProperties properties) {
        return AgentCard.builder()
            .id(properties.getAgent().getId())
            .name(properties.getAgent().getName())
            .description(properties.getAgent().getDescription())
            .version(properties.getAgent().getVersion())
            .capabilities(properties.getAgent().getCapabilities())
            .skills(properties.getAgent().getSkills().stream()
                .map(this::toSkillDefinition)
                .toList())
            .endpoint(properties.getServer().getEndpoint())
            .build();
    }
}
```

## Creating an A2A Agent

### Implementing Task Handler

```java
@Component
public class CustomerSupportTaskHandler implements A2aTaskHandler {

    private final LlmClient llmClient;

    @Override
    public String getSkillName() {
        return "answer-query";
    }

    @Override
    public Flux<TaskUpdate> handleTask(TaskRequest request) {
        String query = request.getInput().get("query").asText();
        String customerId = request.getInput().path("customerId").asText(null);

        return Flux.concat(
            // Send working status
            Flux.just(TaskUpdate.working("Processing query...")),

            // Process the query
            processQuery(query, customerId)
                .map(TaskUpdate::completed)
                .onErrorResume(e ->
                    Mono.just(TaskUpdate.failed(e.getMessage()))
                )
        );
    }

    private Mono<JsonNode> processQuery(String query, String customerId) {
        return llmClient.chat(query)
            .map(response -> objectMapper.createObjectNode()
                .put("answer", response.content())
                .put("customerId", customerId));
    }
}
```

### Streaming Progress

```java
@Override
public Flux<TaskUpdate> handleTask(TaskRequest request) {
    return Flux.create(sink -> {
        sink.next(TaskUpdate.working("Starting analysis..."));

        // Step 1
        sink.next(TaskUpdate.working("Fetching customer data..."));
        CustomerData data = fetchCustomerData(request);

        // Step 2
        sink.next(TaskUpdate.working("Analyzing request..."));
        Analysis analysis = analyzeRequest(request, data);

        // Step 3
        sink.next(TaskUpdate.working("Generating response..."));
        JsonNode result = generateResponse(analysis);

        sink.next(TaskUpdate.completed(result));
        sink.complete();
    });
}
```

## Calling Other Agents

### Agent Discovery

```java
@Service
public class AgentDiscoveryService {

    private final A2aRegistry registry;

    public List<AgentCard> findAgentsByCapability(String capability) {
        return registry.getAgents().stream()
            .filter(agent -> agent.capabilities().contains(capability))
            .toList();
    }

    public Optional<AgentCard> findAgent(String agentId) {
        return registry.getAgent(agentId);
    }
}
```

### Delegating Tasks

```java
@Service
public class TaskDelegationService {

    private final A2aClient a2aClient;

    public Flux<TaskUpdate> delegateTask(
            String targetAgentId,
            String skill,
            JsonNode input) {

        TaskRequest request = TaskRequest.builder()
            .skill(skill)
            .input(input)
            .build();

        return a2aClient.sendTask(targetAgentId, request);
    }

    public Mono<JsonNode> delegateAndWait(
            String targetAgentId,
            String skill,
            JsonNode input) {

        return delegateTask(targetAgentId, skill, input)
            .filter(update -> update.status() == TaskStatus.COMPLETED)
            .next()
            .map(TaskUpdate::result);
    }
}
```

### Multi-Agent Workflow

```java
@Service
public class MultiAgentWorkflow {

    private final A2aClient a2aClient;

    public Flux<WorkflowUpdate> processComplexRequest(ComplexRequest request) {
        return Flux.concat(
            // Step 1: Risk assessment
            delegateToAgent("risk-agent", "assess-risk", request.toJson())
                .map(result -> new WorkflowUpdate("risk", result)),

            // Step 2: Compliance check (parallel with fraud check)
            Flux.merge(
                delegateToAgent("compliance-agent", "check-compliance", request.toJson())
                    .map(result -> new WorkflowUpdate("compliance", result)),
                delegateToAgent("fraud-agent", "check-fraud", request.toJson())
                    .map(result -> new WorkflowUpdate("fraud", result))
            ),

            // Step 3: Final processing
            delegateToAgent("processing-agent", "process", request.toJson())
                .map(result -> new WorkflowUpdate("processing", result))
        );
    }

    private Mono<JsonNode> delegateToAgent(String agent, String skill, JsonNode input) {
        return a2aClient.sendTask(agent, TaskRequest.builder()
                .skill(skill)
                .input(input)
                .build())
            .filter(u -> u.status() == TaskStatus.COMPLETED)
            .next()
            .map(TaskUpdate::result);
    }
}
```

## Task Status Flow

```
SUBMITTED → WORKING → COMPLETED
                   ↘ FAILED
                   ↘ CANCELLED
```

```java
public enum TaskStatus {
    SUBMITTED,    // Task received
    WORKING,      // Processing in progress
    COMPLETED,    // Successfully completed
    FAILED,       // Failed with error
    CANCELLED     // Cancelled by client
}
```

## Error Handling

```java
@Override
public Flux<TaskUpdate> handleTask(TaskRequest request) {
    return processTask(request)
        .onErrorResume(ValidationException.class, e ->
            Flux.just(TaskUpdate.failed("Invalid input: " + e.getMessage()))
        )
        .onErrorResume(ServiceUnavailableException.class, e ->
            Flux.just(TaskUpdate.failed("Service temporarily unavailable"))
        )
        .onErrorResume(Exception.class, e -> {
            log.error("Unexpected error processing task", e);
            return Flux.just(TaskUpdate.failed("Internal error"));
        });
}
```

## Security

### Agent Authentication

```yaml
regulus:
  a2a:
    security:
      enabled: true
      auth-type: mtls  # mtls, jwt, or api-key
      mtls:
        trust-store: classpath:truststore.jks
        key-store: classpath:keystore.jks
```

### Task Authorization

```java
@Component
public class TaskAuthorizationFilter implements A2aFilter {

    @Override
    public Mono<TaskRequest> filter(TaskRequest request, A2aContext context) {
        String callerAgent = context.getCallerAgentId();

        if (!isAuthorized(callerAgent, request.skill())) {
            return Mono.error(new UnauthorizedException(
                "Agent " + callerAgent + " not authorized for skill " + request.skill()
            ));
        }

        return Mono.just(request);
    }
}
```

## Monitoring

### Metrics

- `regulus.a2a.tasks.received.total` - Tasks received
- `regulus.a2a.tasks.sent.total` - Tasks sent to other agents
- `regulus.a2a.tasks.duration` - Task processing duration
- `regulus.a2a.tasks.status` - Tasks by status

```promql
# Task throughput
rate(regulus_a2a_tasks_received_total[5m])

# Task success rate
sum(regulus_a2a_tasks_status{status="COMPLETED"}) /
sum(regulus_a2a_tasks_status)
```

### Distributed Tracing

```java
@Override
public Flux<TaskUpdate> handleTask(TaskRequest request) {
    return Flux.deferContextual(ctx -> {
        Span span = tracer.spanBuilder("a2a.task." + request.skill())
            .setParent(extractContext(request))
            .startSpan();

        return processTask(request)
            .doFinally(signal -> span.end());
    });
}
```

## Testing

```java
@SpringBootTest
class A2aIntegrationTest {

    @Autowired
    private A2aServer server;

    @Autowired
    private A2aClient client;

    @Test
    void shouldProcessTask() {
        JsonNode input = objectMapper.createObjectNode()
            .put("query", "What is my balance?");

        StepVerifier.create(client.sendTask("customer-support-agent",
                TaskRequest.builder()
                    .skill("answer-query")
                    .input(input)
                    .build()))
            .expectNextMatches(u -> u.status() == TaskStatus.WORKING)
            .expectNextMatches(u -> u.status() == TaskStatus.COMPLETED)
            .verifyComplete();
    }
}
```

## Best Practices

1. **Design clear skills** - Each skill should have a single responsibility
2. **Stream progress** - Provide progress updates for long-running tasks
3. **Handle failures gracefully** - Return meaningful error messages
4. **Use timeouts** - Set appropriate timeouts for task delegation
5. **Monitor inter-agent communication** - Track latency and errors
6. **Secure agent communication** - Use mTLS or JWT for authentication
