# First Agent Tutorial

This tutorial walks you through building a complete financial services agent with all core Regulus features.

## What We'll Build

A customer support agent that can:

- Answer questions about account balances
- Process simple requests
- Comply with UK financial regulations
- Handle errors gracefully

## Prerequisites

- Completed [Quick Start](quickstart.md)
- LLM provider configured and working
- Basic Spring Boot knowledge

## Step 1: Project Setup

Extend the quickstart with governance features:

```kotlin title="build.gradle.kts"
dependencies {
    implementation("com.regulus:regulus-ai-agents-spring-boot-starter")
    implementation("com.regulus:regulus-ai-safety-starter")
    implementation("com.regulus:regulus-ai-governance-starter")  // Add this

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

## Step 2: Configure the Agent

```yaml title="application.yml"
regulus:
  llm:
    provider: gemini
    gemini:
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
      model: gemini-2.0-flash

  # Policy configuration
  policy:
    require-purpose-code: true
    require-consent: true
    allowed-purpose-codes:
      - CUSTOMER_SUPPORT
      - ACCOUNT_INQUIRY

  # Privacy configuration
  privacy:
    redaction:
      enabled: true
      patterns:
        - nino
        - sort-code
        - account-number
        - credit-card
        - phone-uk
        - email

  # Kill switch configuration
  kill-switch:
    enabled: true
    check-interval: 30s
    backend: config  # or redis, consul

  # Model registry for SS1/23
  model-registry:
    enabled: true
    model-id: customer-support-agent-v1
    risk-tier: MEDIUM
    owner: ai-platform-team
    review-cadence: QUARTERLY

spring:
  application:
    name: customer-support-agent
```

## Step 3: Create Domain Models

```java title="src/main/java/com/example/model/CustomerContext.java"
package com.example.model;

public record CustomerContext(
    String customerId,
    String purposeCode,
    boolean hasConsent,
    String sessionId
) {
    public static CustomerContext forSupport(String customerId, String sessionId) {
        return new CustomerContext(
            customerId,
            "CUSTOMER_SUPPORT",
            true,
            sessionId
        );
    }
}
```

```java title="src/main/java/com/example/model/AgentRequest.java"
package com.example.model;

public record AgentRequest(
    String message,
    CustomerContext context
) {}
```

```java title="src/main/java/com/example/model/AgentResponse.java"
package com.example.model;

import java.time.Instant;

public record AgentResponse(
    String content,
    String sessionId,
    Instant timestamp,
    ResponseMetadata metadata
) {
    public record ResponseMetadata(
        String modelId,
        int tokenCount,
        long latencyMs
    ) {}
}
```

## Step 4: Create the Agent Service

```java title="src/main/java/com/example/service/CustomerSupportAgent.java"
package com.example.service;

import com.example.model.*;
import com.neullabs.regulus.ai.llm.LlmClient;
import com.neullabs.regulus.ai.llm.ChatMessage;
import com.neullabs.regulus.ai.policy.PolicyGuard;
import com.neullabs.regulus.ai.policy.PolicyContext;
import com.neullabs.regulus.ai.privacy.PrivacyFilter;
import com.neullabs.regulus.ai.killswitch.KillSwitch;
import com.neullabs.regulus.ai.observability.AuditLogger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class CustomerSupportAgent {

    private static final String SYSTEM_PROMPT = """
        You are a helpful customer support agent for a UK bank.

        Guidelines:
        - Be polite and professional
        - Never share sensitive customer data
        - If you don't know something, say so
        - Refer complex issues to human agents

        You can help with:
        - Account balance inquiries
        - Transaction history questions
        - General banking questions
        """;

    private final LlmClient llmClient;
    private final PolicyGuard policyGuard;
    private final PrivacyFilter privacyFilter;
    private final KillSwitch killSwitch;
    private final AuditLogger auditLogger;

    public CustomerSupportAgent(
            LlmClient llmClient,
            PolicyGuard policyGuard,
            PrivacyFilter privacyFilter,
            KillSwitch killSwitch,
            AuditLogger auditLogger) {
        this.llmClient = llmClient;
        this.policyGuard = policyGuard;
        this.privacyFilter = privacyFilter;
        this.killSwitch = killSwitch;
        this.auditLogger = auditLogger;
    }

    public Mono<AgentResponse> process(AgentRequest request) {
        long startTime = System.currentTimeMillis();

        // Check kill switch first
        if (killSwitch.isActive()) {
            return Mono.error(new ServiceUnavailableException(
                "Service temporarily unavailable"
            ));
        }

        // Build policy context
        PolicyContext policyContext = PolicyContext.builder()
            .purposeCode(request.context().purposeCode())
            .hasConsent(request.context().hasConsent())
            .userId(request.context().customerId())
            .build();

        // Redact PII from input
        String sanitizedInput = privacyFilter.redact(request.message());

        // Log the request
        auditLogger.logRequest(request.context().sessionId(), sanitizedInput);

        return policyGuard.enforce(policyContext)
            .then(callLlm(sanitizedInput))
            .map(response -> {
                // Redact PII from response
                String sanitizedOutput = privacyFilter.redact(response);
                long latency = System.currentTimeMillis() - startTime;

                // Log the response
                auditLogger.logResponse(
                    request.context().sessionId(),
                    sanitizedOutput,
                    latency
                );

                return new AgentResponse(
                    sanitizedOutput,
                    request.context().sessionId(),
                    Instant.now(),
                    new AgentResponse.ResponseMetadata(
                        llmClient.getModel(),
                        response.length() / 4, // Approximate token count
                        latency
                    )
                );
            })
            .onErrorResume(e -> {
                auditLogger.logError(request.context().sessionId(), e);
                return Mono.error(e);
            });
    }

    private Mono<String> callLlm(String userMessage) {
        List<ChatMessage> messages = List.of(
            ChatMessage.system(SYSTEM_PROMPT),
            ChatMessage.user(userMessage)
        );

        return llmClient.chat(messages)
            .map(ChatResponse::content);
    }
}
```

## Step 5: Create the Controller

```java title="src/main/java/com/example/controller/AgentController.java"
package com.example.controller;

import com.example.model.*;
import com.example.service.CustomerSupportAgent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final CustomerSupportAgent agent;

    public AgentController(CustomerSupportAgent agent) {
        this.agent = agent;
    }

    @PostMapping(
        value = "/chat",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<AgentResponse> chat(@RequestBody AgentRequest request) {
        return agent.process(request);
    }

    @GetMapping("/health")
    public Mono<HealthResponse> health() {
        return Mono.just(new HealthResponse("UP", "customer-support-agent"));
    }
}

record HealthResponse(String status, String service) {}
```

## Step 6: Add Error Handling

```java title="src/main/java/com/example/exception/GlobalExceptionHandler.java"
package com.example.exception;

import com.neullabs.regulus.ai.policy.PolicyViolationException;
import com.neullabs.regulus.ai.killswitch.KillSwitchActiveException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<ErrorResponse> handlePolicyViolation(
            PolicyViolationException e) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(
                "POLICY_VIOLATION",
                e.getMessage(),
                Instant.now()
            ));
    }

    @ExceptionHandler(KillSwitchActiveException.class)
    public ResponseEntity<ErrorResponse> handleKillSwitch(
            KillSwitchActiveException e) {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(
                "SERVICE_UNAVAILABLE",
                "Service is temporarily unavailable",
                Instant.now()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                Instant.now()
            ));
    }
}

record ErrorResponse(String code, String message, Instant timestamp) {}
```

## Step 7: Test the Agent

Run the application:

```bash
./gradlew bootRun
```

Test basic chat:

```bash
curl -X POST http://localhost:8080/api/v1/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is my account balance?",
    "context": {
      "customerId": "CUST-12345",
      "purposeCode": "CUSTOMER_SUPPORT",
      "hasConsent": true,
      "sessionId": "sess-abc123"
    }
  }'
```

Test PII redaction:

```bash
curl -X POST http://localhost:8080/api/v1/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "My NINO is AB123456C, can you check my account?",
    "context": {
      "customerId": "CUST-12345",
      "purposeCode": "CUSTOMER_SUPPORT",
      "hasConsent": true,
      "sessionId": "sess-abc456"
    }
  }'
```

Test policy violation (missing consent):

```bash
curl -X POST http://localhost:8080/api/v1/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Check my balance",
    "context": {
      "customerId": "CUST-12345",
      "purposeCode": "CUSTOMER_SUPPORT",
      "hasConsent": false,
      "sessionId": "sess-abc789"
    }
  }'
```

## What You've Built

Your agent now includes:

- **LLM Integration**: Configured with your chosen provider
- **Policy Guards**: Enforcing purpose codes and consent
- **PII Redaction**: Automatic masking of sensitive data
- **Kill Switch**: Ready for emergency shutdown
- **Audit Logging**: All interactions logged
- **Error Handling**: Graceful error responses

## Next Steps

- [Policy Guards](../guides/core-features/policy-guards.md) - Advanced policy configuration
- [Kill Switch Operations](../guides/core-features/kill-switch.md) - Dual-control setup
- [Monitoring](../guides/operations/monitoring.md) - Observability setup
