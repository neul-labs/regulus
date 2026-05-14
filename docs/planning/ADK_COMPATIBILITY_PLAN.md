# Regulus - Google ADK Compatibility Plan

## Goal

Ensure Regulus agents are **fully interoperable** with the Google ADK ecosystem via MCP and A2A protocols, while adding UK financial services governance as a compliance layer.

**Philosophy**: Protocol-compatible, not library-dependent. Regulus speaks the same language as ADK agents without requiring Google's Python libraries.

---

## Current State Assessment

### What Already Works ✅

| Component | Status | ADK Compatibility |
|-----------|--------|-------------------|
| MCP Client (`HttpMcpClient`) | ✅ Implemented | Can consume any MCP server |
| MCP Server (`McpServerController`) | ✅ Implemented | Can be consumed by ADK agents |
| A2A Client (`HttpA2aClient`) | ✅ Implemented | Can discover and call ADK agents |
| A2A Server (`A2aServerController`) | ✅ Implemented | Exposes `/.well-known/agent.json` |
| Agent Card | ✅ Implemented | Standard A2A agent card format |
| MCP-to-A2A Bridge | ✅ Implemented | Exposes MCP tools as A2A skills |
| JSON-RPC 2.0 | ✅ Implemented | Protocol compliant |

### Protocol Versions

| Protocol | Regulus Version | ADK Version | Compatible? |
|----------|-----------------|-------------|-------------|
| MCP | `2024-11-05` | `2024-11-05` | ✅ Yes |
| A2A | `0.1` | `0.1` | ✅ Yes |

---

## Gaps for Full ADK Compatibility

### Gap 1: LLM Provider Support (Including Gemini)
**Priority**: 🔴 HIGH
**Effort**: 1 week

ADK agents typically use Gemini. To be a first-class ADK citizen, Regulus should support:
- **Google Gemini** via Vertex AI
- **OpenAI** (already planned)
- **Anthropic** (already planned)

**Tasks**:
- [ ] Add `google-cloud-vertexai` dependency
- [ ] Implement `GeminiLlmClient` using LangChain4j Vertex AI module
- [ ] Add Gemini configuration properties
- [ ] Test with Gemini Pro and Gemini Flash

**Files to create**:
```
platform/core/regulus-ai-llm/
└── src/main/java/com/neullabs/regulus/llm/provider/
    └── GeminiLlmClient.java
```

**Configuration**:
```yaml
regulus:
  ai:
    llm:
      providers:
        gemini:
          enabled: true
          project-id: ${GCP_PROJECT_ID}
          location: europe-west2
          model: gemini-1.5-pro
```

---

### Gap 2: MCP Streaming Support
**Priority**: 🟠 MEDIUM
**Effort**: 3 days

ADK supports streaming responses for long-running tool executions. Current Regulus implementation is request/response only.

**Tasks**:
- [ ] Add Server-Sent Events (SSE) support to `McpServerController`
- [ ] Add SSE client support to `HttpMcpClient`
- [ ] Implement `notifications/progress` MCP method
- [ ] Support partial result streaming

**ADK MCP Methods to Add**:
```
notifications/progress    - Stream progress updates
notifications/message     - Stream messages during execution
```

---

### Gap 3: A2A Task Streaming
**Priority**: 🟠 MEDIUM
**Effort**: 3 days

ADK A2A supports streaming task updates. Current implementation polls for status.

**Tasks**:
- [ ] Add SSE endpoint for task streaming: `GET /tasks/{taskId}/stream`
- [ ] Implement push notifications for task state changes
- [ ] Add streaming support to `HttpA2aClient`

---

### Gap 4: MCP Resource Protocol
**Priority**: 🟡 LOW
**Effort**: 1 week

MCP has a `resources` capability for exposing data sources (documents, databases). Regulus only implements `tools`.

**ADK MCP Capabilities**:
```
tools       ✅ Implemented
resources   ❌ Not implemented
prompts     ❌ Not implemented
```

**Tasks** (if needed for specific use case):
- [ ] Implement `resources/list` method
- [ ] Implement `resources/read` method
- [ ] Add resource registration API
- [ ] Integrate with vector stores / document retrieval

---

### Gap 5: Authentication Alignment
**Priority**: 🟠 MEDIUM
**Effort**: 3 days

Ensure authentication methods match ADK expectations:
- OAuth 2.0 bearer tokens
- Google Cloud IAM service accounts
- API keys (for development)

**Tasks**:
- [ ] Add Google Cloud IAM token validation
- [ ] Support `Authorization: Bearer` with Google-issued tokens
- [ ] Document authentication patterns for ADK interop

---

## Revised Implementation Plan

### Sprint 1: Gemini + Core LLM (1 week)

**Goal**: Enable Regulus agents to use Gemini as their LLM

| Task | Effort | Priority |
|------|--------|----------|
| Create `regulus-ai-llm` module | 2 days | 🔴 |
| Implement `LlmClient` interface | 1 day | 🔴 |
| Implement `GeminiLlmClient` with Vertex AI | 2 days | 🔴 |
| Add OpenAI + Anthropic providers | 2 days | 🔴 |
| Wire into agent execution pipeline | 1 day | 🔴 |

**Deliverable**: Quickstart demo using Gemini

---

### Sprint 2: MCP/A2A Streaming (1 week)

**Goal**: Full streaming support for long-running operations

| Task | Effort | Priority |
|------|--------|----------|
| MCP SSE server support | 2 days | 🟠 |
| MCP SSE client support | 1 day | 🟠 |
| A2A task streaming endpoint | 2 days | 🟠 |
| Integration tests with ADK client | 2 days | 🟠 |

**Deliverable**: Can stream responses to/from ADK agents

---

### Sprint 3: Authentication + Hardening (1 week)

**Goal**: Production-ready auth compatible with GCP

| Task | Effort | Priority |
|------|--------|----------|
| Google IAM token validation | 2 days | 🟠 |
| Service account authentication | 1 day | 🟠 |
| mTLS for inter-agent calls | 2 days | 🟠 |
| Audit logging for all ADK calls | 1 day | 🟠 |
| Database persistence for audit | 1 day | 🟠 |

**Deliverable**: Secure ADK interop with audit trail

---

### Sprint 4: Testing + Documentation (1 week)

**Goal**: Prove compatibility with real ADK agents

| Task | Effort | Priority |
|------|--------|----------|
| Integration tests with ADK Python agent | 2 days | 🟠 |
| Contract tests for MCP/A2A | 2 days | 🟠 |
| ADK compatibility guide | 1 day | 🟡 |
| Example: Regulus consuming ADK agent | 1 day | 🟡 |
| Example: ADK consuming Regulus tools | 1 day | 🟡 |

**Deliverable**: Documented, tested ADK interoperability

---

## What We're NOT Building

To stay focused on ADK compatibility, we're **deferring** these from the original gap plan:

| Original Gap | Decision | Reason |
|--------------|----------|--------|
| Full PostgreSQL persistence | Defer | Start with in-memory, add DB later |
| Complex RBAC system | Defer | OAuth + basic roles sufficient for now |
| GDPR workflows | Defer | Not needed for ADK interop |
| SS1/23 model registry | Defer | Compliance layer, not interop |
| Grafana dashboards | Defer | Nice-to-have |
| Vault secrets | Defer | Use env vars / GCP Secret Manager |

---

## Architecture: ADK-Compatible Agent

```
┌─────────────────────────────────────────────────────────────┐
│                    Regulus Agent                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Gemini    │  │   OpenAI    │  │     Anthropic       │ │
│  │   Client    │  │   Client    │  │      Client         │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
│         └────────────────┼────────────────────┘            │
│                          ▼                                  │
│                   ┌──────────────┐                         │
│                   │  LLM Router  │                         │
│                   └──────┬───────┘                         │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Governance Layer                        │   │
│  │  ┌──────────┐ ┌───────────┐ ┌────────────────────┐  │   │
│  │  │ Policy   │ │  Privacy  │ │    Kill Switch     │  │   │
│  │  │ Guards   │ │  Filters  │ │    Interceptor     │  │   │
│  │  └──────────┘ └───────────┘ └────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           Protocol Layer (ADK Compatible)            │   │
│  │  ┌─────────────────┐    ┌─────────────────────────┐ │   │
│  │  │   MCP Server    │    │      A2A Server         │ │   │
│  │  │   /mcp          │    │  /.well-known/agent.json│ │   │
│  │  │   (tools)       │    │  /tasks                 │ │   │
│  │  └─────────────────┘    └─────────────────────────┘ │   │
│  │  ┌─────────────────┐    ┌─────────────────────────┐ │   │
│  │  │   MCP Client    │    │      A2A Client         │ │   │
│  │  │  (consume ADK)  │    │   (discover ADK agents) │ │   │
│  │  └─────────────────┘    └─────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ MCP/A2A (HTTP + JSON-RPC 2.0)
                              ▼
                    ┌───────────────────┐
                    │   Google ADK      │
                    │   Agents          │
                    │   (Python)        │
                    └───────────────────┘
```

---

## Quick Start: ADK Interop Demo

After implementing Sprint 1, the quickstart will work like this:

### 1. Regulus Agent Using Gemini
```yaml
# application.yaml
regulus:
  ai:
    llm:
      default-provider: gemini
      providers:
        gemini:
          enabled: true
          project-id: my-gcp-project
          location: europe-west2
          model: gemini-1.5-flash
    mcp:
      enabled: true
      server:
        enabled: true
```

### 2. Consuming External ADK Agent
```java
// Discover and use an ADK agent's tools
A2aClient a2aClient = new HttpA2aClient();
A2aAgent adkAgent = a2aClient.discoverAgent("https://adk-agent.example.com").join();

// Call the ADK agent's skill
A2aTaskResponse response = a2aClient.sendTask(
    adkAgent.url(),
    A2aTask.builder()
        .skillId("analyze_document")
        .input(Map.of("document_url", "gs://bucket/doc.pdf"))
        .build()
).join();
```

### 3. Exposing Regulus Tools to ADK
```bash
# ADK agent can discover Regulus tools at:
curl https://regulus-agent.example.com/.well-known/agent.json

# And invoke tools via MCP:
curl -X POST https://regulus-agent.example.com/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"iso20022_validate","arguments":{...}},"id":1}'
```

---

## Success Criteria

| Criterion | Metric |
|-----------|--------|
| Gemini integration works | Can generate responses using Gemini Pro |
| Can consume ADK MCP server | Successfully call tools on Python ADK agent |
| ADK can consume Regulus MCP | Python ADK agent can use Regulus tools |
| A2A discovery works | ADK agent can fetch Regulus agent card |
| A2A task execution works | End-to-end task between Regulus and ADK |
| Governance applies | All ADK calls pass through policy guards |

---

## Timeline Summary

| Sprint | Duration | Focus | Outcome |
|--------|----------|-------|---------|
| Sprint 1 | 1 week | Gemini + LLM layer | Working LLM integration |
| Sprint 2 | 1 week | Streaming | Full MCP/A2A streaming |
| Sprint 3 | 1 week | Auth + hardening | Production-ready security |
| Sprint 4 | 1 week | Testing + docs | Proven ADK compatibility |

**Total**: 4 weeks to full ADK compatibility

---

## References

- [Google ADK Documentation](https://google.github.io/adk-docs/)
- [MCP Specification](https://google.github.io/adk-docs/mcp/)
- [A2A Protocol](https://google.github.io/adk-docs/a2a/)
- [Vertex AI Java SDK](https://cloud.google.com/vertex-ai/docs/reference/java)
- [LangChain4j Vertex AI Module](https://docs.langchain4j.dev/integrations/language-models/google-vertex-ai-gemini)

---

*Document Version: 1.0*
*Created: 2024-12-09*
*Focus: Google ADK Ecosystem Compatibility*
