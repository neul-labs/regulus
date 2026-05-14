# DSL Configuration

Configure agents using Kotlin or YAML DSLs.

## Overview

Regulus supports two DSL formats for agent configuration:

- **Kotlin DSL** - Type-safe, IDE support, compile-time validation
- **YAML DSL** - Human-readable, easy to edit, runtime validation

## Kotlin DSL

### Basic Agent Configuration

```kotlin
// src/main/kotlin/com/example/AgentConfig.kt
import com.neullabs.regulus.dsl.agent
import com.neullabs.regulus.dsl.tool
import com.neullabs.regulus.dsl.policy

val customerSupportAgent = agent("customer-support") {
    description = "Customer support agent for banking inquiries"

    llm {
        provider = "gemini"
        model = "gemini-2.0-flash"
        temperature = 0.7
    }

    systemPrompt = """
        You are a helpful customer support agent for a UK bank.
        Be polite, professional, and helpful.
        Never share sensitive customer data.
    """.trimIndent()

    tools {
        tool("get_balance") {
            description = "Get account balance"
            parameter("accountId", String::class) {
                description = "The account ID"
                required = true
            }
            handler = { params -> accountService.getBalance(params["accountId"]) }
        }

        tool("get_transactions") {
            description = "Get recent transactions"
            parameter("accountId", String::class) { required = true }
            parameter("limit", Int::class) { default = 10 }
            handler = { params ->
                accountService.getTransactions(
                    params["accountId"],
                    params["limit"] ?: 10
                )
            }
        }
    }

    policies {
        policy("purpose-code") {
            require { context -> context.purposeCode in allowedPurposeCodes }
            onViolation { "Invalid purpose code" }
        }

        policy("consent") {
            require { context -> context.hasConsent }
            onViolation { "Consent required" }
        }
    }

    privacy {
        redact("nino", "sort-code", "account-number")
        customPattern("employee-id", "EMP-\\d{6}")
    }

    observability {
        metrics = true
        tracing = true
        auditLogging = true
    }
}
```

### Registering the Agent

```kotlin
@Configuration
class AgentConfiguration {

    @Bean
    fun customerSupport(): Agent {
        return customerSupportAgent.build()
    }
}
```

### Multi-Agent Configuration

```kotlin
val agents = agents {
    agent("customer-support") {
        description = "Handles customer inquiries"
        // ...
    }

    agent("payment-processor") {
        description = "Processes payments"
        // ...
    }

    agent("fraud-detector") {
        description = "Detects fraudulent activity"
        // ...
    }

    // Define agent interactions
    interactions {
        "customer-support" canCall "payment-processor"
        "customer-support" canCall "fraud-detector"
    }
}
```

## YAML DSL

### Basic Configuration

```yaml
# src/main/resources/agents/customer-support.yml
agent:
  id: customer-support
  description: Customer support agent for banking inquiries

  llm:
    provider: gemini
    model: gemini-2.0-flash
    temperature: 0.7
    max-tokens: 4096

  system-prompt: |
    You are a helpful customer support agent for a UK bank.
    Be polite, professional, and helpful.
    Never share sensitive customer data.

  tools:
    - name: get_balance
      description: Get account balance
      parameters:
        - name: accountId
          type: string
          required: true
          description: The account ID

    - name: get_transactions
      description: Get recent transactions
      parameters:
        - name: accountId
          type: string
          required: true
        - name: limit
          type: integer
          required: false
          default: 10

  policies:
    require-purpose-code: true
    require-consent: true
    allowed-purpose-codes:
      - CUSTOMER_SUPPORT
      - ACCOUNT_INQUIRY

  privacy:
    redaction:
      enabled: true
      patterns:
        - nino
        - sort-code
        - account-number
      custom:
        - name: employee-id
          pattern: "EMP-\\d{6}"
          replacement: "[EMPLOYEE ID REDACTED]"

  observability:
    metrics: true
    tracing: true
    audit-logging: true
```

### Loading YAML Configuration

```java
@Configuration
public class YamlAgentConfig {

    @Bean
    public Agent customerSupportAgent(
            @Value("classpath:agents/customer-support.yml") Resource config) {
        return YamlAgentLoader.load(config);
    }
}
```

### Multiple Agents

```yaml
# src/main/resources/agents/agents.yml
agents:
  - id: customer-support
    description: Handles customer inquiries
    llm:
      provider: gemini
      model: gemini-2.0-flash
    system-prompt: |
      You are a customer support agent...

  - id: payment-processor
    description: Processes payments
    llm:
      provider: gemini
      model: gemini-2.0-flash
    system-prompt: |
      You are a payment processing agent...

interactions:
  customer-support:
    can-call:
      - payment-processor
      - fraud-detector
```

## Pipeline Configuration

### Kotlin Pipeline DSL

```kotlin
val supportPipeline = pipeline("support-pipeline") {
    // Input preprocessing
    stage("preprocess") {
        redactPii()
        validateInput()
    }

    // Policy enforcement
    stage("policy") {
        enforcePolicies()
    }

    // LLM processing
    stage("llm") {
        callLlm {
            model = "gemini-2.0-flash"
            temperature = 0.7
        }
    }

    // Tool execution
    stage("tools") {
        executeTools()
    }

    // Output postprocessing
    stage("postprocess") {
        redactPii()
        addDisclosure()
    }

    // Error handling
    onError { error ->
        when (error) {
            is PolicyViolationException -> fallbackToError(error.message)
            is LlmException -> retry(maxAttempts = 3)
            else -> escalateToHuman()
        }
    }
}
```

### YAML Pipeline

```yaml
pipeline:
  id: support-pipeline

  stages:
    - name: preprocess
      actions:
        - redact-pii
        - validate-input

    - name: policy
      actions:
        - enforce-policies

    - name: llm
      config:
        model: gemini-2.0-flash
        temperature: 0.7

    - name: tools
      actions:
        - execute-tools

    - name: postprocess
      actions:
        - redact-pii
        - add-disclosure

  error-handling:
    policy-violation:
      action: fallback-error
    llm-error:
      action: retry
      max-attempts: 3
    default:
      action: escalate-human
```

## RAG Configuration

### Kotlin RAG DSL

```kotlin
val ragAgent = agent("knowledge-base") {
    description = "Knowledge base agent with RAG"

    rag {
        vectorStore {
            type = "pinecone"
            index = "knowledge-base"
            namespace = "banking-faqs"
        }

        embedding {
            provider = "openai"
            model = "text-embedding-3-small"
        }

        retrieval {
            topK = 5
            similarityThreshold = 0.7
            reranking = true
        }

        chunking {
            strategy = "semantic"
            maxChunkSize = 500
            overlap = 50
        }
    }

    systemPrompt = """
        Answer questions using the retrieved context.
        If the context doesn't contain the answer, say so.
    """.trimIndent()
}
```

### YAML RAG

```yaml
agent:
  id: knowledge-base
  description: Knowledge base agent with RAG

  rag:
    vector-store:
      type: pinecone
      index: knowledge-base
      namespace: banking-faqs

    embedding:
      provider: openai
      model: text-embedding-3-small

    retrieval:
      top-k: 5
      similarity-threshold: 0.7
      reranking: true

    chunking:
      strategy: semantic
      max-chunk-size: 500
      overlap: 50

  system-prompt: |
    Answer questions using the retrieved context.
    If the context doesn't contain the answer, say so.
```

## Validation

### Kotlin DSL Validation

```kotlin
val agent = customerSupportAgent.build()

// Validate at build time
agent.validate().let { result ->
    if (!result.isValid) {
        result.errors.forEach { error ->
            logger.error("Validation error: ${error.message}")
        }
        throw ConfigurationException("Agent configuration invalid")
    }
}
```

### YAML Validation

```java
@Component
public class YamlValidator {

    public ValidationResult validate(Resource yamlResource) {
        AgentConfig config = loadConfig(yamlResource);

        List<String> errors = new ArrayList<>();

        if (config.getAgentId() == null) {
            errors.add("Agent ID is required");
        }

        if (config.getLlm() == null) {
            errors.add("LLM configuration is required");
        }

        // More validations...

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
```

## Environment Variable Interpolation

### YAML with Environment Variables

```yaml
agent:
  id: customer-support

  llm:
    provider: ${LLM_PROVIDER:gemini}
    model: ${LLM_MODEL:gemini-2.0-flash}

  api-keys:
    openai: ${OPENAI_API_KEY}
    gemini: ${GOOGLE_CLOUD_PROJECT}
```

### Kotlin with Environment Variables

```kotlin
val agent = agent("customer-support") {
    llm {
        provider = env("LLM_PROVIDER", "gemini")
        model = env("LLM_MODEL", "gemini-2.0-flash")
    }
}
```

## Best Practices

1. **Use Kotlin for complex logic** - Type safety catches errors early
2. **Use YAML for simple configs** - Easy to read and modify
3. **Validate early** - Check configuration at startup
4. **Externalize secrets** - Use environment variables
5. **Version control** - Track configuration changes
6. **Test configurations** - Unit test DSL configurations
