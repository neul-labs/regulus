# ADR-003: MCP Protocol for Tool Exposure

## Status
Accepted

## Date
2025-01-10

## Context

AI agents need to interact with external systems and tools. In UK financial services, these tools include:
- Core banking systems (account lookups, transactions)
- Credit reference agencies (Experian, Equifax)
- Market data providers (Bloomberg, Reuters)
- Internal systems (CRM, document management)
- Regulatory reporting systems

The platform needs a standardized way to:
1. Expose tools to AI agents securely
2. Apply governance controls (authentication, authorization, audit)
3. Enable tool discovery and documentation
4. Support multiple tool providers and consumers

Several options exist for tool protocols:
- **Custom REST APIs**: Bespoke per-tool, no standardization
- **OpenAPI/Swagger**: Good for REST, limited AI agent support
- **Function Calling**: Provider-specific (OpenAI, Anthropic differ)
- **MCP (Model Context Protocol)**: Emerging standard from Anthropic

## Decision

We will adopt the **Model Context Protocol (MCP)** as the standard protocol for exposing tools to AI agents, with the following implementation:

### Core Design

1. **JSON-RPC 2.0 transport**: Standard request/response format
2. **Three protocol types**: Tools, Resources, and Prompts
3. **HTTP/SSE transport**: RESTful with Server-Sent Events for streaming
4. **Governance integration**: Policy guards applied at protocol level
5. **Auto-discovery**: Tools registered via Spring component scanning

### Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         MCP Server                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    McpProtocolHandler                        │  │
│  │                                                              │  │
│  │   initialize ──▶ ServerCapabilities                         │  │
│  │   tools/list ──▶ Tool Registry                              │  │
│  │   tools/call ──▶ Tool Execution ──▶ Policy Guards           │  │
│  │   resources/list ──▶ Resource Registry                      │  │
│  │   resources/read ──▶ Resource Handler                       │  │
│  │   prompts/list ──▶ Prompt Registry                          │  │
│  │   prompts/get ──▶ Prompt Handler                            │  │
│  │                                                              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                              │                                      │
│              ┌───────────────┼───────────────┐                     │
│              ▼               ▼               ▼                     │
│      ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│      │   @McpTool  │ │@McpResource │ │ @McpPrompt  │              │
│      │   Handlers  │ │  Handlers   │ │  Handlers   │              │
│      └─────────────┘ └─────────────┘ └─────────────┘              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Protocol Implementation

#### Tools Protocol
```java
@Component
@McpTool(
    name = "get_account_balance",
    description = "Retrieve current balance for a customer account"
)
public class AccountBalanceTool implements McpToolHandler {

    @Override
    public JsonNode getInputSchema() {
        return JsonSchema.object()
            .required("account_id", JsonSchema.string())
            .optional("currency", JsonSchema.string())
            .build();
    }

    @Override
    @RequireLEI  // Governance annotation
    @AuditLogged
    public McpToolResult execute(Map<String, Object> arguments) {
        String accountId = (String) arguments.get("account_id");
        // Implementation with full audit trail
        return McpToolResult.success(balance);
    }
}
```

#### Resources Protocol
```java
@Component
@McpResource(
    uri = "regulatory://fca/consumer-duty/guidance",
    name = "FCA Consumer Duty Guidance",
    mimeType = "text/markdown"
)
public class ConsumerDutyResource implements McpResourceHandler {

    @Override
    public ResourceContent read() {
        return ResourceContent.text(loadGuidanceDocument());
    }
}
```

#### Prompts Protocol
```java
@Component
@McpPrompt(
    name = "affordability_assessment",
    description = "Structured prompt for mortgage affordability assessment"
)
public class AffordabilityPrompt implements McpPromptHandler {

    @Override
    public List<PromptArgument> getArguments() {
        return List.of(
            PromptArgument.required("income", "Annual gross income"),
            PromptArgument.required("expenses", "Monthly expenses"),
            PromptArgument.optional("dependents", "Number of dependents")
        );
    }

    @Override
    public PromptResult get(Map<String, String> arguments) {
        return PromptResult.messages(
            buildAssessmentPrompt(arguments)
        );
    }
}
```

### Governance Integration

```
┌─────────────────────────────────────────────────────────┐
│                  MCP Request Flow                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Request ──▶ Authentication ──▶ Authorization           │
│                                      │                  │
│                                      ▼                  │
│                              Policy Guards              │
│                              (@RequireLEI,              │
│                               @RequirePurposeCode,      │
│                               @DataResidency)           │
│                                      │                  │
│                                      ▼                  │
│                              Tool Execution             │
│                                      │                  │
│                                      ▼                  │
│                              Audit Logging              │
│                                      │                  │
│                                      ▼                  │
│                              Response                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Consequences

### Positive

1. **Standardization**: Single protocol for all tool interactions
2. **Ecosystem compatibility**: Works with MCP-compatible clients
3. **Governance integration**: Policy guards apply uniformly
4. **Discovery**: Auto-registration and listing of capabilities
5. **Flexibility**: Tools, Resources, and Prompts cover most use cases
6. **Streaming support**: SSE for real-time updates

### Negative

1. **Protocol overhead**: JSON-RPC adds parsing overhead vs direct calls
2. **Emerging standard**: MCP is relatively new, may evolve
3. **Learning curve**: Teams must learn MCP concepts
4. **Limited tooling**: Fewer debugging tools than REST/OpenAPI

### Mitigations

| Concern | Mitigation |
|---------|------------|
| Performance | Connection pooling, response caching where appropriate |
| Protocol evolution | Version capability negotiation, backwards compatibility |
| Learning curve | Comprehensive documentation, examples, starter templates |
| Debugging | Custom MCP debugging dashboard, structured logging |

## Alternatives Considered

### 1. Custom REST APIs per Tool

**Pros**: Full control, familiar technology
**Cons**: No standardization, each tool requires custom integration
**Decision**: Rejected - doesn't scale, no unified governance

### 2. OpenAI Function Calling Format

**Pros**: Well-documented, wide adoption
**Cons**: Provider-specific, doesn't include resources/prompts
**Decision**: Rejected - too tightly coupled to single provider

### 3. LangChain Tool Format

**Pros**: Popular in Python ecosystem
**Cons**: Python-centric, limited Java support
**Decision**: Rejected - platform is Java-based

### 4. gRPC Protocol

**Pros**: High performance, strong typing
**Cons**: Complex setup, limited browser support, not AI-native
**Decision**: Rejected - not designed for AI agent use case

### 5. GraphQL

**Pros**: Flexible queries, good tooling
**Cons**: Not designed for tool execution, no streaming standard
**Decision**: Rejected - wrong paradigm for tool invocation

## References

- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- [MCP Integration Guide](../architecture/adk-mcp-a2a.md)
- [Quickstart Tutorial](../guides/quickstart-tutorial.md)
