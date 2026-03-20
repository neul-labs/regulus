# Quick Start

Build your first compliant AI agent in 5 minutes.

## Create a New Project

### Option 1: Clone the Quickstart Example

```bash
git clone https://github.com/Skelf-Research/regulus.git
cd regulus/examples/quickstart
```

### Option 2: Start from Scratch

Create a new Spring Boot project:

```bash
mkdir my-agent && cd my-agent
```

Create the build file:

```kotlin title="build.gradle.kts"
plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
    java
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("com.regulus:regulus-ai-bom:0.1.0-SNAPSHOT")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.regulus:regulus-ai-agents-spring-boot-starter")
    implementation("com.regulus:regulus-ai-safety-starter")
}
```

## Configure Your LLM Provider

Create `src/main/resources/application.yml`:

=== "Google Vertex AI"

    ```yaml
    regulus:
      llm:
        provider: gemini
        gemini:
          project-id: ${GOOGLE_CLOUD_PROJECT}
          location: europe-west2
          model: gemini-2.0-flash
    ```

=== "OpenAI"

    ```yaml
    regulus:
      llm:
        provider: openai
        openai:
          api-key: ${OPENAI_API_KEY}
          model: gpt-4o
    ```

=== "Anthropic"

    ```yaml
    regulus:
      llm:
        provider: anthropic
        anthropic:
          api-key: ${ANTHROPIC_API_KEY}
          model: claude-3-5-sonnet-20241022
    ```

## Create the Application

```java title="src/main/java/com/example/Application.java"
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## Create the Agent Controller

```java title="src/main/java/com/example/AgentController.java"
package com.example;

import com.regulus.ai.llm.LlmClient;
import com.regulus.ai.policy.PolicyGuard;
import com.regulus.ai.privacy.PrivacyFilter;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final LlmClient llmClient;
    private final PolicyGuard policyGuard;
    private final PrivacyFilter privacyFilter;

    public AgentController(
            LlmClient llmClient,
            PolicyGuard policyGuard,
            PrivacyFilter privacyFilter) {
        this.llmClient = llmClient;
        this.policyGuard = policyGuard;
        this.privacyFilter = privacyFilter;
    }

    @PostMapping("/chat")
    public Mono<ChatResponse> chat(@RequestBody ChatRequest request) {
        // 1. Redact PII from input
        String sanitizedInput = privacyFilter.redact(request.message());

        // 2. Enforce policy guards
        return policyGuard.enforce(request)
            // 3. Call LLM
            .flatMap(validated -> llmClient.chat(sanitizedInput))
            // 4. Redact PII from output
            .map(response -> new ChatResponse(
                privacyFilter.redact(response.content())
            ));
    }
}

record ChatRequest(String message, String userId) {}
record ChatResponse(String content) {}
```

## Run the Application

```bash
./gradlew bootRun
```

## Test Your Agent

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, what can you help me with?", "userId": "user123"}'
```

Expected response:

```json
{
  "content": "I'm an AI assistant that can help you with financial services questions..."
}
```

## What's Included

Your quickstart agent includes:

| Feature | Description |
|---------|-------------|
| **LLM Integration** | Configured provider with streaming support |
| **Privacy Filter** | Automatic PII redaction (NINO, account numbers, etc.) |
| **Policy Guards** | Basic policy enforcement |
| **Audit Logging** | All interactions logged for compliance |
| **Health Checks** | Spring Actuator endpoints |

## Test PII Redaction

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "My NINO is AB123456C and my sort code is 12-34-56", "userId": "user123"}'
```

The NINO and sort code will be redacted before processing.

## Next Steps

- [First Agent Tutorial](first-agent.md) - Complete walkthrough
- [Policy Guards](../guides/core-features/policy-guards.md) - Configure compliance rules
- [Kill Switch](../guides/core-features/kill-switch.md) - Add emergency controls
