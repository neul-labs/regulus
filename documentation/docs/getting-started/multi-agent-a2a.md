# Multi-agent with A2A

Two ADK agents talking over A2A, with the Regulus envelope on every hop.

## Why this matters

Production agents end up calling other agents. The same regulator question
— "is personal data redacted before it leaves the JVM?" — has to keep
being "yes" across hops. The A2A envelope is how.

## 1. Two Spring Boot modules

Easiest setup: two services in one repo, deployed separately.

```kotlin
// examples/adk-multi-agent-a2a/build.gradle.kts
dependencies {
    implementation("com.google.adk:google-adk:1.2.0")
    implementation(platform("com.regulus.platform:regulus-ai-bom:0.1.0"))
    implementation("com.regulus.platform:regulus-ai-adk-spring-boot-starter")
    implementation("com.regulus.platform:regulus-ai-adk-a2a")
}
```

## 2. Expose the A2A endpoint on the callee

In the agent that gets called:

```java
@Bean
AgentExecutor a2aExecutor(RegulusAgentExecutor regulus) {
    return regulus;  // RegulusAgentExecutor extends/wraps the ADK class
}
```

The ADK A2A JSON-RPC handler is now wrapped. Inbound calls run through:
policy + privacy + kill-switch + audit, in that order, before reaching the
agent.

## 3. Call from the caller

```java
RegulusRemoteA2AAgent decisioner = new RegulusRemoteA2AAgent(
    URI.create("https://decisioner.internal/a2a"),
    auditSink,
    /* signRequests = */ true);

LlmAgent intake = LlmAgent.builder()
    .name("intake")
    .subAgent(decisioner)        // remote agent as a sub-agent
    .build();
```

Outbound calls run through: policy + privacy + audit on the request side;
audit on the response side. If signing is enabled, the receiving end can
verify the caller.

## 4. Watch the joint trail

Both sides emit audit events with the same `correlation_id` so the trail
reconstructs end-to-end:

```
[intake]     a2a-outbound  callee=decisioner  redactions=[NINO_1]
[decisioner] a2a-inbound   caller=intake      redactions=[NINO_1]
[decisioner] model-call    model=gemini-2.5-flash
[decisioner] a2a-response  caller=intake      redactions=[NINO_1]
[intake]     a2a-inbound-response             redactions=[NINO_1]
```

## Cross-organisation A2A

If the two agents belong to different organisations, set
`signRequests = true` and configure key trust. The signed request body
carries the caller's identity so the audit trail can include cross-org
attribution. Identity federation beyond that uses standard transport-layer
mechanisms (mTLS, OIDC).

## Next

- [Deploy to Vertex AI Agent Engine](vertex-deploy.md)
- [Services → A2A](../services/a2a.md)
