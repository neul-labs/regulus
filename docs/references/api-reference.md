# API Reference

Complete API reference for Regulus platform components. All APIs follow RESTful conventions unless otherwise specified.

---

## Quick Reference

| Component | Base Path | Protocol | Auth Required |
|-----------|-----------|----------|---------------|
| Agent API | `/api/agent` | REST | JWT |
| MCP Server | `/mcp` | JSON-RPC 2.0 | JWT/mTLS |
| Admin API | `/api/admin` | REST | JWT (Admin role) |
| Health & Metrics | `/actuator` | REST | None/Basic |

---

## Agent API

### POST /api/agent/{agentId}/chat

Send a message to an AI agent and receive a response.

**Request:**

```http
POST /api/agent/mortgage-adviser/chat HTTP/1.1
Host: api.yourbank.com
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "message": "What is the maximum mortgage I can afford with £75,000 income?",
  "conversationId": "conv-12345",
  "context": {
    "customerId": "CUST-67890",
    "channel": "mobile-app"
  }
}
```

**Response:**

```json
{
  "responseId": "resp-abc123",
  "conversationId": "conv-12345",
  "message": "Based on standard affordability calculations...",
  "agentId": "mortgage-adviser",
  "timestamp": "2025-01-15T10:30:00Z",
  "metadata": {
    "tokensUsed": 245,
    "latencyMs": 1250,
    "model": "gemini-1.5-pro"
  }
}
```

**Error Responses:**

| Status | Code | Description |
|--------|------|-------------|
| 400 | `INVALID_REQUEST` | Malformed request body |
| 401 | `UNAUTHORIZED` | Missing or invalid token |
| 403 | `FORBIDDEN` | Insufficient permissions |
| 404 | `AGENT_NOT_FOUND` | Agent ID not registered |
| 429 | `RATE_LIMITED` | Too many requests |
| 503 | `KILL_SWITCH_ACTIVE` | Agent is disabled |

---

### POST /api/agent/{agentId}/stream

Stream agent responses using Server-Sent Events.

**Request:**

```http
POST /api/agent/mortgage-adviser/stream HTTP/1.1
Host: api.yourbank.com
Authorization: Bearer <jwt-token>
Content-Type: application/json
Accept: text/event-stream

{
  "message": "Explain the mortgage application process",
  "conversationId": "conv-12345"
}
```

**Response (SSE):**

```
event: token
data: {"token": "The"}

event: token
data: {"token": " mortgage"}

event: token
data: {"token": " application"}

event: complete
data: {"responseId": "resp-abc123", "tokensUsed": 450}
```

---

### GET /api/agent/{agentId}/status

Get agent operational status.

**Response:**

```json
{
  "agentId": "mortgage-adviser",
  "status": "ACTIVE",
  "killSwitchState": {
    "global": false,
    "agentType": false,
    "agent": false
  },
  "lastActivity": "2025-01-15T10:30:00Z",
  "metrics": {
    "requestsLast24h": 1250,
    "avgLatencyMs": 1100,
    "errorRate": 0.02
  }
}
```

**Status Values:**

| Status | Description |
|--------|-------------|
| `ACTIVE` | Agent accepting requests |
| `DEGRADED` | Agent operational but with issues |
| `DISABLED` | Kill switch active |
| `MAINTENANCE` | Planned maintenance |

---

## MCP Protocol API

### POST /mcp

JSON-RPC 2.0 endpoint for Model Context Protocol.

#### initialize

Initialize MCP session and negotiate capabilities.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {},
      "resources": {},
      "prompts": {}
    },
    "clientInfo": {
      "name": "mortgage-adviser-agent",
      "version": "1.0.0"
    }
  }
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": { "listChanged": true },
      "resources": { "subscribe": true, "listChanged": true },
      "prompts": { "listChanged": true }
    },
    "serverInfo": {
      "name": "regulus-mcp-server",
      "version": "1.0.0"
    }
  }
}
```

---

#### tools/list

List available tools.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list"
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "calculate_affordability",
        "description": "Calculate mortgage affordability based on income and expenses",
        "inputSchema": {
          "type": "object",
          "properties": {
            "annual_income": {
              "type": "number",
              "description": "Annual gross income in GBP"
            },
            "monthly_expenses": {
              "type": "number",
              "description": "Monthly committed expenses in GBP"
            },
            "deposit": {
              "type": "number",
              "description": "Available deposit in GBP"
            }
          },
          "required": ["annual_income", "monthly_expenses", "deposit"]
        }
      },
      {
        "name": "get_interest_rates",
        "description": "Get current mortgage interest rates",
        "inputSchema": {
          "type": "object",
          "properties": {
            "mortgage_type": {
              "type": "string",
              "enum": ["fixed", "variable", "tracker"]
            },
            "term_years": {
              "type": "integer",
              "minimum": 2,
              "maximum": 10
            }
          },
          "required": ["mortgage_type"]
        }
      }
    ]
  }
}
```

---

#### tools/call

Execute a tool.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "calculate_affordability",
    "arguments": {
      "annual_income": 75000,
      "monthly_expenses": 1500,
      "deposit": 50000
    }
  }
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Based on your income of £75,000 and monthly expenses of £1,500, you could afford a mortgage of approximately £337,500 (4.5x income). With your £50,000 deposit, you could purchase a property valued at £387,500."
      }
    ],
    "isError": false
  }
}
```

**Error Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Error: LEI validation required for this operation"
      }
    ],
    "isError": true
  }
}
```

---

#### resources/list

List available resources.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "resources/list"
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "result": {
    "resources": [
      {
        "uri": "regulatory://fca/consumer-duty/guidance",
        "name": "FCA Consumer Duty Guidance",
        "description": "Current FCA guidance on Consumer Duty requirements",
        "mimeType": "text/markdown"
      },
      {
        "uri": "policy://mortgage/affordability-criteria",
        "name": "Mortgage Affordability Criteria",
        "description": "Internal lending criteria and affordability rules",
        "mimeType": "application/json"
      }
    ]
  }
}
```

---

#### resources/read

Read a resource.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "resources/read",
  "params": {
    "uri": "policy://mortgage/affordability-criteria"
  }
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "result": {
    "contents": [
      {
        "uri": "policy://mortgage/affordability-criteria",
        "mimeType": "application/json",
        "text": "{\"maxLtvRatio\": 0.95, \"incomeMultiplier\": 4.5, \"stressRate\": 0.03}"
      }
    ]
  }
}
```

---

#### prompts/list

List available prompt templates.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 6,
  "method": "prompts/list"
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 6,
  "result": {
    "prompts": [
      {
        "name": "affordability_assessment",
        "description": "Structured prompt for mortgage affordability assessment",
        "arguments": [
          {
            "name": "income",
            "description": "Annual gross income",
            "required": true
          },
          {
            "name": "expenses",
            "description": "Monthly expenses",
            "required": true
          },
          {
            "name": "dependents",
            "description": "Number of dependents",
            "required": false
          }
        ]
      }
    ]
  }
}
```

---

#### prompts/get

Get a rendered prompt.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "prompts/get",
  "params": {
    "name": "affordability_assessment",
    "arguments": {
      "income": "75000",
      "expenses": "1500"
    }
  }
}
```

**Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "result": {
    "description": "Affordability assessment for customer",
    "messages": [
      {
        "role": "user",
        "content": {
          "type": "text",
          "text": "Please assess mortgage affordability for a customer with annual income of £75,000 and monthly expenses of £1,500. Apply standard FCA affordability rules and stress testing."
        }
      }
    ]
  }
}
```

---

## Admin API

### Kill Switch

#### POST /api/admin/kill-switch/activate

Request kill switch activation (requires second approver).

**Request:**

```json
{
  "scope": "GLOBAL",
  "target": null,
  "reason": "Model drift detected - accuracy below threshold",
  "emergency": false
}
```

**Response:**

```json
{
  "requestId": "KS-20250115-001",
  "status": "PENDING_APPROVAL",
  "scope": "GLOBAL",
  "requestedBy": "operator-a@bank.com",
  "requestedAt": "2025-01-15T10:30:00Z",
  "expiresAt": "2025-01-15T14:30:00Z"
}
```

**Scope Values:**

| Scope | Target | Description |
|-------|--------|-------------|
| `GLOBAL` | null | All agents |
| `AGENT_TYPE` | Agent type name | All agents of type |
| `AGENT` | Agent ID | Specific agent |

---

#### POST /api/admin/kill-switch/approve/{requestId}

Approve a pending kill switch request.

**Request:**

```http
POST /api/admin/kill-switch/approve/KS-20250115-001 HTTP/1.1
Authorization: Bearer <jwt-token-for-approver>
```

**Response:**

```json
{
  "requestId": "KS-20250115-001",
  "status": "EXECUTED",
  "approvedBy": "operator-b@bank.com",
  "approvedAt": "2025-01-15T10:35:00Z",
  "executedAt": "2025-01-15T10:35:00Z"
}
```

---

#### GET /api/admin/kill-switch/status

Get current kill switch status.

**Response:**

```json
{
  "global": {
    "active": false,
    "lastChange": null
  },
  "agentTypes": {
    "mortgage-adviser": {
      "active": false,
      "lastChange": null
    }
  },
  "agents": {},
  "pendingRequests": [
    {
      "requestId": "KS-20250115-002",
      "scope": "AGENT_TYPE",
      "target": "investment-adviser",
      "reason": "Compliance review required",
      "requestedBy": "risk-team@bank.com",
      "requestedAt": "2025-01-15T09:00:00Z",
      "expiresAt": "2025-01-15T13:00:00Z"
    }
  ]
}
```

---

#### GET /api/admin/kill-switch/audit

Get kill switch audit log.

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `from` | ISO8601 | 24h ago | Start time |
| `to` | ISO8601 | now | End time |
| `scope` | string | all | Filter by scope |
| `limit` | int | 100 | Max results |

**Response:**

```json
{
  "entries": [
    {
      "timestamp": "2025-01-15T10:35:00Z",
      "action": "ACTIVATED",
      "scope": "GLOBAL",
      "target": null,
      "requestedBy": "operator-a@bank.com",
      "approvedBy": "operator-b@bank.com",
      "reason": "Model drift detected",
      "emergency": false
    }
  ],
  "total": 1,
  "from": "2025-01-14T10:30:00Z",
  "to": "2025-01-15T10:30:00Z"
}
```

---

### Data Residency

#### GET /api/admin/data-residency/status

Get data residency configuration and status.

**Response:**

```json
{
  "enforced": true,
  "allowedRegions": [
    {
      "provider": "gcp",
      "region": "europe-west2",
      "location": "London, UK"
    },
    {
      "provider": "aws",
      "region": "eu-west-2",
      "location": "London, UK"
    },
    {
      "provider": "azure",
      "region": "uksouth",
      "location": "London, UK"
    }
  ],
  "violations": {
    "last24h": 0,
    "last7d": 2,
    "last30d": 5
  },
  "lastCheck": "2025-01-15T10:30:00Z"
}
```

---

#### GET /api/admin/data-residency/violations

Get data residency violation log.

**Response:**

```json
{
  "violations": [
    {
      "timestamp": "2025-01-14T15:22:00Z",
      "attemptedRegion": "us-central1",
      "dataClassification": "PII",
      "endpoint": "https://us-central1-aiplatform.googleapis.com/...",
      "blocked": true,
      "source": "mortgage-adviser-agent"
    }
  ],
  "total": 2
}
```

---

### Model Registry

#### GET /api/admin/models

List registered models.

**Response:**

```json
{
  "models": [
    {
      "id": "mortgage-adviser",
      "name": "Mortgage Adviser Agent",
      "version": "1.2.0",
      "status": "DEPLOYED",
      "riskTier": "TIER_2",
      "owner": "Lending Team",
      "lastValidation": "2025-01-10T09:00:00Z",
      "nextReview": "2025-04-10T09:00:00Z",
      "metrics": {
        "accuracy": 0.94,
        "latencyP95Ms": 1500,
        "requestsLast30d": 45000
      }
    }
  ],
  "total": 5
}
```

---

#### GET /api/admin/models/{modelId}

Get detailed model information.

**Response:**

```json
{
  "id": "mortgage-adviser",
  "name": "Mortgage Adviser Agent",
  "version": "1.2.0",
  "status": "DEPLOYED",
  "riskTier": "TIER_2",
  "metadata": {
    "owner": "Lending Team",
    "intendedUse": "Customer-facing mortgage affordability assessments",
    "reviewCadence": "QUARTERLY",
    "materialityThreshold": "HIGH"
  },
  "validation": {
    "lastValidation": "2025-01-10T09:00:00Z",
    "validatedBy": "model-validation-team@bank.com",
    "validationReport": "https://docs.internal/validation/mortgage-adviser-v1.2.0.pdf",
    "challengerModel": "mortgage-adviser-challenger-v1"
  },
  "performance": {
    "accuracy": 0.94,
    "precision": 0.92,
    "recall": 0.96,
    "driftScore": 0.02,
    "lastDriftCheck": "2025-01-15T00:00:00Z"
  },
  "lineage": {
    "baseModel": "gemini-1.5-pro",
    "trainingData": "mortgage-applications-2024",
    "finetuned": false
  },
  "auditTrail": [
    {
      "timestamp": "2025-01-10T09:00:00Z",
      "action": "VALIDATED",
      "actor": "model-validation-team@bank.com"
    },
    {
      "timestamp": "2025-01-05T14:00:00Z",
      "action": "DEPLOYED",
      "actor": "platform-team@bank.com"
    }
  ]
}
```

---

## Health & Metrics

### GET /actuator/health

Application health status.

**Response:**

```json
{
  "status": "UP",
  "components": {
    "killSwitch": {
      "status": "UP",
      "details": {
        "globalActive": false,
        "pendingRequests": 0
      }
    },
    "dataResidency": {
      "status": "UP",
      "details": {
        "enforced": true,
        "violationsLast24h": 0
      }
    },
    "llm": {
      "status": "UP",
      "details": {
        "primaryProvider": "gemini",
        "latencyMs": 1100
      }
    },
    "db": {
      "status": "UP"
    }
  }
}
```

---

### GET /actuator/prometheus

Prometheus metrics endpoint.

**Key Metrics:**

```prometheus
# Agent request metrics
regulus_agent_requests_total{agent="mortgage-adviser",status="success"} 15234
regulus_agent_requests_total{agent="mortgage-adviser",status="error"} 42
regulus_agent_latency_seconds{agent="mortgage-adviser",quantile="0.95"} 1.5

# Kill switch metrics
regulus_kill_switch_active{scope="global"} 0
regulus_kill_switch_activations_total 3

# Data residency metrics
regulus_data_residency_checks_total{result="allowed"} 45000
regulus_data_residency_checks_total{result="blocked"} 2

# LLM metrics
regulus_llm_requests_total{provider="gemini",model="gemini-1.5-pro"} 45000
regulus_llm_tokens_total{provider="gemini",type="input"} 2250000
regulus_llm_tokens_total{provider="gemini",type="output"} 1125000
regulus_llm_latency_seconds{provider="gemini",quantile="0.95"} 1.2

# MCP metrics
regulus_mcp_tool_calls_total{tool="calculate_affordability"} 12000
regulus_mcp_tool_latency_seconds{tool="calculate_affordability",quantile="0.95"} 0.25
```

---

## Error Codes

### Standard Error Response

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Annual income must be a positive number",
    "details": {
      "field": "annual_income",
      "value": -50000,
      "constraint": "positive"
    },
    "traceId": "abc123def456"
  }
}
```

### Error Code Reference

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_REQUEST` | 400 | Malformed request |
| `VALIDATION_ERROR` | 400 | Input validation failed |
| `UNAUTHORIZED` | 401 | Authentication required |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `RATE_LIMITED` | 429 | Rate limit exceeded |
| `KILL_SWITCH_ACTIVE` | 503 | Agent disabled |
| `DATA_RESIDENCY_VIOLATION` | 403 | Region not allowed |
| `LLM_ERROR` | 502 | LLM provider error |
| `INTERNAL_ERROR` | 500 | Unexpected error |

---

## SDK Examples

### Java

```java
// Using Regulus Java SDK
RegulusClient client = RegulusClient.builder()
    .baseUrl("https://api.yourbank.com")
    .apiKey(System.getenv("REGULUS_API_KEY"))
    .build();

// Chat with agent
ChatResponse response = client.chat()
    .agentId("mortgage-adviser")
    .message("What mortgage can I afford?")
    .context(Map.of("customerId", "CUST-123"))
    .execute();

// Stream response
client.stream()
    .agentId("mortgage-adviser")
    .message("Explain the application process")
    .onToken(token -> System.out.print(token))
    .onComplete(resp -> System.out.println("\nDone: " + resp.getResponseId()))
    .execute();
```

### Python

```python
from regulus import RegulusClient

client = RegulusClient(
    base_url="https://api.yourbank.com",
    api_key=os.environ["REGULUS_API_KEY"]
)

# Chat with agent
response = client.chat(
    agent_id="mortgage-adviser",
    message="What mortgage can I afford?",
    context={"customer_id": "CUST-123"}
)

# Stream response
for token in client.stream(
    agent_id="mortgage-adviser",
    message="Explain the application process"
):
    print(token, end="", flush=True)
```

---

## Related Documentation

- [Quickstart Tutorial](../guides/quickstart-tutorial.md)
- [MCP Integration Guide](../architecture/adk-mcp-a2a.md)
- [Security Hardening](../guides/security-hardening.md)
- [Troubleshooting](../guides/troubleshooting.md)
