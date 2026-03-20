# ADK, MCP, and A2A Integration

Regulus builds on Google's Agent Development Kit (ADK) to deliver interoperable agents that can both consume and expose capabilities via the Model Context Protocol (MCP) and Agent-to-Agent (A2A) protocol.

---

## ADK Integration

- **Agent Classes**: ADK agents, planners, memory providers, and tools are auto-wired through the `ai-agents-spring-boot-starter`.
- **Policy Guards**: Aspect-oriented interceptors wrap every tool invocation to enforce LEI, purpose codes, consent, and other governance controls.
- **Privacy Shim**: Redacts PII in prompts, MCP payloads, and A2A messages; attaches purpose and retention metadata for downstream auditing.
- **Observability Hooks**: Emits Micrometer/OTEL spans for `llm.call`, `route.slm|llm`, `mcp.tool`, and `a2a.call`; Kafka audit events capture who/what/when for compliance.

---

## LLM Streaming

Regulus supports real-time token streaming for all major LLM providers, essential for responsive user experiences.

### Supported Providers

| Provider | Streaming Class | Region Support |
|----------|-----------------|----------------|
| Google Gemini | `GeminiLlmClient` | europe-west2 (London) |
| OpenAI | `OpenAiLlmClient` | Via Azure UK South |
| Anthropic | `AnthropicLlmClient` | Via AWS eu-west-2 |

### Streaming Configuration

```yaml
regulus:
  ai:
    llm:
      provider: gemini
      streaming:
        enabled: true
      gemini:
        project-id: ${GCP_PROJECT_ID}
        location: europe-west2
        model: gemini-1.5-pro
```

### Java API

```java
@Autowired
private LlmClient llmClient;

// Stream tokens with callback
llmClient.streamChat(messages, new TokenStreamHandler() {
    @Override
    public void onToken(String token) {
        // Process each token as it arrives
        responseBuilder.append(token);
        sendToClient(token);
    }

    @Override
    public void onComplete(LlmResponse response) {
        // Final response with metadata
        log.info("Completed: {} tokens", response.tokenCount());
    }

    @Override
    public void onError(Throwable error) {
        log.error("Stream error", error);
    }
});
```

---

## MCP Protocol

The Model Context Protocol (MCP) provides JSON-RPC 2.0 over HTTP for tool discovery and invocation. Regulus implements the full MCP specification including **Tools**, **Resources**, and **Prompts**.

### MCP Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       MCP SERVER                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   TOOLS     │  │  RESOURCES  │  │   PROMPTS   │             │
│  │             │  │             │  │             │             │
│  │ tools/list  │  │ resources/  │  │ prompts/    │             │
│  │ tools/call  │  │   list      │  │   list      │             │
│  │             │  │ resources/  │  │ prompts/    │             │
│  │             │  │   read      │  │   get       │             │
│  │             │  │ resources/  │  │             │             │
│  │             │  │   subscribe │  │             │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    STREAMING (SSE)                       │   │
│  │  /mcp/stream/events  |  /mcp/stream/tools/call          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### MCP Tools

Tools are functions that agents can discover and invoke.

#### Exposing Tools

```java
@McpTool(
    name = "calculate_mortgage_affordability",
    description = "Calculate mortgage affordability based on income and expenses"
)
public class MortgageAffordabilityTool implements McpToolHandler {

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        BigDecimal income = new BigDecimal(arguments.get("annual_income").toString());
        BigDecimal expenses = new BigDecimal(arguments.get("monthly_expenses").toString());

        // FCA affordability calculation
        BigDecimal maxMortgage = calculateAffordability(income, expenses);

        return McpToolResult.success(Map.of(
            "max_mortgage", maxMortgage,
            "ltv_ratio", 0.85,
            "assessment_date", Instant.now()
        ));
    }
}
```

#### Tool Discovery (JSON-RPC)

```json
// Request: tools/list
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list"
}

// Response
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "calculate_mortgage_affordability",
        "description": "Calculate mortgage affordability based on income and expenses",
        "inputSchema": {
          "type": "object",
          "properties": {
            "annual_income": { "type": "number" },
            "monthly_expenses": { "type": "number" }
          },
          "required": ["annual_income", "monthly_expenses"]
        }
      }
    ]
  }
}
```

### MCP Resources

Resources provide read-only access to data sources like documents, configurations, or knowledge bases.

#### Defining Resources

```java
@Component
public class ComplianceDocumentProvider implements McpResourceProvider {

    @Override
    public String getProviderId() {
        return "compliance-docs";
    }

    @Override
    public ResourceList listResources(String cursor, int limit) {
        List<McpResource> resources = List.of(
            new McpResource(
                "compliance://fca/consumer-duty",
                "FCA Consumer Duty Guidance",
                "FG22/5 Consumer Duty guidance for firms",
                "text/markdown",
                Map.of("version", "2023-07", "regulator", "FCA")
            ),
            new McpResource(
                "compliance://pra/ss1-23",
                "PRA SS1/23 Model Risk",
                "Model risk management principles for banks",
                "text/markdown",
                Map.of("version", "2023-05", "regulator", "PRA")
            )
        );
        return new ResourceList(resources, null); // null cursor = no more pages
    }

    @Override
    public Optional<ResourceContent> readResource(String uri) {
        if (uri.equals("compliance://fca/consumer-duty")) {
            return Optional.of(new ResourceContent(
                uri,
                "text/markdown",
                loadDocument("consumer-duty.md")
            ));
        }
        return Optional.empty();
    }
}
```

#### Resource Discovery (JSON-RPC)

```json
// Request: resources/list
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "resources/list"
}

// Response
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "resources": [
      {
        "uri": "compliance://fca/consumer-duty",
        "name": "FCA Consumer Duty Guidance",
        "description": "FG22/5 Consumer Duty guidance for firms",
        "mimeType": "text/markdown"
      }
    ]
  }
}

// Request: resources/read
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "resources/read",
  "params": { "uri": "compliance://fca/consumer-duty" }
}
```

#### Resource Subscriptions

Resources support real-time change notifications:

```java
// Subscribe to resource changes
mcpResourceManager.subscribe("compliance://fca/*", (uri, changeType) -> {
    log.info("Resource changed: {} ({})", uri, changeType);
    // Refresh cached content
});
```

### MCP Prompts

Prompts are reusable templates that can be filled with arguments.

#### Defining Prompts

```java
@Component
public class CompliancePromptProvider {

    @Bean
    public McpPrompt suitabilityAssessmentPrompt() {
        return new McpPrompt(
            "suitability_assessment",
            "Generate a suitability assessment for a financial product recommendation",
            List.of(
                new McpPrompt.Argument("customer_profile", "Customer risk profile and circumstances", true),
                new McpPrompt.Argument("product_type", "Type of product being recommended", true),
                new McpPrompt.Argument("recommendation_rationale", "Why this product is suitable", true)
            )
        );
    }
}
```

#### Prompt Manager

```java
@Autowired
private McpPromptManager promptManager;

// Register a prompt handler
promptManager.registerHandler("suitability_assessment", arguments -> {
    String customerProfile = (String) arguments.get("customer_profile");
    String productType = (String) arguments.get("product_type");
    String rationale = (String) arguments.get("recommendation_rationale");

    String expandedPrompt = String.format("""
        Generate a suitability assessment document for FCA compliance.

        Customer Profile:
        %s

        Product Type: %s

        Recommendation Rationale:
        %s

        The assessment must address:
        1. Customer's investment objectives
        2. Risk tolerance alignment
        3. Financial situation appropriateness
        4. Consumer Duty considerations
        """, customerProfile, productType, rationale);

    return new McpPromptManager.PromptResult(
        "Suitability Assessment Template",
        List.of(new McpPromptManager.PromptMessage("user", expandedPrompt))
    );
});

// Execute a prompt
Optional<McpPromptManager.PromptResult> result = promptManager.executePrompt(
    "suitability_assessment",
    Map.of(
        "customer_profile", "Moderate risk, 15-year horizon, ISA wrapper preference",
        "product_type", "Global Equity Fund",
        "recommendation_rationale", "Diversified exposure aligned with growth objective"
    )
);
```

### MCP Streaming (SSE)

Real-time streaming for long-running tool operations:

```java
// Server-Sent Events endpoint
@GetMapping(path = "/mcp/stream/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamEvents(@RequestParam String clientId) {
    return mcpStreamingController.streamEvents(clientId);
}

// Streaming tool execution
@PostMapping("/mcp/stream/tools/execute/{toolName}")
public Flux<ServerSentEvent<String>> executeToolStreaming(
    @PathVariable String toolName,
    @RequestBody Map<String, Object> arguments
) {
    return mcpStreamingController.executeToolStreaming(toolName, arguments);
}
```

Client-side consumption:

```javascript
const eventSource = new EventSource('/mcp/stream/events?clientId=my-agent');

eventSource.addEventListener('tool_progress', (event) => {
    const data = JSON.parse(event.data);
    console.log('Progress:', data.progress, '%');
});

eventSource.addEventListener('tool_result', (event) => {
    const result = JSON.parse(event.data);
    console.log('Result:', result);
    eventSource.close();
});
```

---

## A2A Protocol

Agent-to-Agent (A2A) protocol enables collaboration between agents across teams and organisations.

### A2A Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        A2A PROTOCOL                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Agent A (Mortgage Adviser)        Agent B (Credit Risk)        │
│  ┌─────────────────────┐          ┌─────────────────────┐      │
│  │                     │          │                     │      │
│  │  "I need a credit   │ ──A2A──▶ │  "Processing credit │      │
│  │   assessment for    │          │   assessment..."    │      │
│  │   customer X"       │          │                     │      │
│  │                     │ ◀─────── │  Task updates via   │      │
│  │                     │  Stream  │  SSE streaming      │      │
│  │                     │          │                     │      │
│  └─────────────────────┘          └─────────────────────┘      │
│                                                                 │
│  Both agents enforce:                                           │
│  - Policy guards (LEI, purpose codes)                          │
│  - Privacy filters (PII redaction)                             │
│  - Kill switch controls                                        │
│  - Audit logging                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### A2A Server Configuration

```yaml
regulus:
  ai:
    a2a:
      server:
        enabled: true
        path: /a2a
        agent-card:
          name: credit-risk-agent
          description: Performs credit risk assessments
          version: 1.0.0
          capabilities:
            - credit_scoring
            - affordability_check
            - fraud_detection
      streaming:
        enabled: true
```

### A2A Task Submission

```java
@Autowired
private A2aClient a2aClient;

// Submit a task to another agent
A2aTaskRequest request = A2aTaskRequest.builder()
    .targetAgent("credit-risk-agent")
    .taskType("credit_assessment")
    .input(Map.of(
        "customer_id", "CUST-12345",
        "loan_amount", 250000,
        "loan_term_months", 300
    ))
    .metadata(Map.of(
        "lei", "5493001KJTIIGC8Y1R12",
        "purpose_code", "MORTGAGE_APPLICATION"
    ))
    .build();

A2aTaskResponse response = a2aClient.submitTask(request);
String taskId = response.getTaskId();
```

### A2A Streaming

Real-time task progress via Server-Sent Events:

```java
// Stream task updates
@GetMapping(path = "/a2a/tasks/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamTaskUpdates(@PathVariable String taskId) {
    return Flux.create(sink -> {
        // Send progress updates
        sink.next(ServerSentEvent.builder()
            .event("task_progress")
            .data("{\"taskId\":\"" + taskId + "\",\"status\":\"IN_PROGRESS\",\"progress\":25}")
            .build());

        // Send artifacts as they're generated
        sink.next(ServerSentEvent.builder()
            .event("task_artifact")
            .data("{\"type\":\"credit_score\",\"value\":720}")
            .build());

        // Send completion
        sink.next(ServerSentEvent.builder()
            .event("task_complete")
            .data("{\"taskId\":\"" + taskId + "\",\"status\":\"COMPLETED\"}")
            .build());

        sink.complete();
    });
}
```

### Agent Card Discovery

```json
// GET /a2a/.well-known/agent.json
{
  "name": "credit-risk-agent",
  "description": "Performs credit risk assessments for UK mortgage applications",
  "version": "1.0.0",
  "provider": {
    "organisation": "Acme Bank",
    "contact": "ai-platform@acmebank.com"
  },
  "capabilities": [
    {
      "name": "credit_scoring",
      "description": "Calculate credit score based on bureau data",
      "inputSchema": {
        "type": "object",
        "properties": {
          "customer_id": { "type": "string" }
        }
      }
    }
  ],
  "authentication": {
    "type": "oauth2",
    "scopes": ["credit:read", "credit:assess"]
  }
}
```

---

## GCP Native Authentication

Regulus supports native GCP authentication for Vertex AI and other Google Cloud services.

### Authentication Modes

| Mode | Use Case | Configuration |
|------|----------|---------------|
| `APPLICATION_DEFAULT` | Local dev with `gcloud auth` | Default |
| `SERVICE_ACCOUNT_FILE` | Service account JSON key | Set `service-account-file` |
| `METADATA_SERVER` | GKE/Cloud Run | Automatic |
| `COMPUTE_ENGINE` | GCE VMs | Automatic |

### Configuration

```yaml
regulus:
  ai:
    gcp:
      authentication:
        mode: APPLICATION_DEFAULT
        service-account-file: ${GCP_SA_KEY_PATH:}
        scopes:
          - https://www.googleapis.com/auth/cloud-platform
```

### Java API

```java
@Autowired
private GcpCredentialsProvider credentialsProvider;

// Get credentials for API calls
GoogleCredentials credentials = credentialsProvider.getCredentials();

// Refresh if needed
if (credentials.getAccessToken().getExpirationTime().before(new Date())) {
    credentials.refresh();
}
```

---

## Compliance Envelope

All MCP/A2A interactions are governed by Regulus compliance controls:

| Control | MCP | A2A | Purpose |
|---------|-----|-----|---------|
| Policy Guards | Yes | Yes | LEI, purpose code, consent enforcement |
| Privacy Filters | Yes | Yes | PII redaction in payloads |
| Kill Switch | Yes | Yes | Emergency disablement |
| Audit Logging | Yes | Yes | Who/what/when for compliance |
| Rate Limiting | Yes | Yes | DoS protection |
| mTLS | Yes | Yes | Transport security |
| Data Residency | Yes | Yes | UK region enforcement |

### Audit Event Schema

```json
{
  "eventType": "mcp.tool.call",
  "timestamp": "2025-01-15T10:30:00Z",
  "correlationId": "req-abc123",
  "source": {
    "agent": "mortgage-adviser",
    "version": "1.2.0"
  },
  "target": {
    "server": "credit-risk-mcp",
    "tool": "credit_assessment"
  },
  "governance": {
    "lei": "5493001KJTIIGC8Y1R12",
    "purposeCode": "MORTGAGE_APPLICATION",
    "dataResidency": "europe-west2"
  },
  "outcome": "SUCCESS",
  "durationMs": 245
}
```

---

## References

- [ADK documentation](https://google.github.io/adk-docs/)
- [MCP specification](https://spec.modelcontextprotocol.io/)
- [A2A protocol](https://google.github.io/adk-docs/a2a/)
- [Google developer blog on A2A](https://developers.googleblog.com/en/a2a-a-new-era-of-agent-interoperability/)

---

## Related Documentation

- [Spring Boot Starters](../guides/starters.md)
- [Data Residency Guide](../guides/data-residency.md)
- [Kill Switch Design](../governance/kill-switch.md)
- [Quickstart Tutorial](../guides/quickstart-tutorial.md)

