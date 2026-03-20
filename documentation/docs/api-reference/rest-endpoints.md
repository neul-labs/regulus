# REST Endpoints

Complete REST API reference.

## Agent Chat API

### POST /api/v1/agent/chat

Send a message to the agent and receive a response.

**Request:**

```http
POST /api/v1/agent/chat HTTP/1.1
Host: agent.regulus.internal
Authorization: Bearer <token>
Content-Type: application/json

{
  "message": "What is my account balance?",
  "context": {
    "customerId": "CUST-12345",
    "purposeCode": "CUSTOMER_SUPPORT",
    "hasConsent": true,
    "sessionId": "sess-abc123"
  }
}
```

**Response:**

```json
{
  "content": "I'd be happy to help you check your account balance. Based on your records, your current account balance is displayed in your online banking portal. Would you like me to guide you through accessing it?",
  "sessionId": "sess-abc123",
  "timestamp": "2024-01-15T10:30:00Z",
  "metadata": {
    "modelId": "gemini-2.0-flash",
    "tokenCount": 45,
    "latencyMs": 892
  }
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `message` | string | Yes | User message |
| `context.customerId` | string | Yes | Customer identifier |
| `context.purposeCode` | string | Yes | Purpose code for the interaction |
| `context.hasConsent` | boolean | Yes | Whether consent was obtained |
| `context.sessionId` | string | No | Session ID for conversation tracking |

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `content` | string | Agent response |
| `sessionId` | string | Session identifier |
| `timestamp` | string | ISO 8601 timestamp |
| `metadata.modelId` | string | LLM model used |
| `metadata.tokenCount` | integer | Tokens in response |
| `metadata.latencyMs` | integer | Processing time in milliseconds |

**Error Responses:**

| Status | Code | Description |
|--------|------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request body |
| 403 | `POLICY_VIOLATION` | Policy check failed |
| 503 | `SERVICE_UNAVAILABLE` | Kill switch active |

---

### POST /api/v1/agent/stream

Stream a response from the agent using Server-Sent Events.

**Request:**

```http
POST /api/v1/agent/stream HTTP/1.1
Host: agent.regulus.internal
Authorization: Bearer <token>
Content-Type: application/json
Accept: text/event-stream

{
  "message": "Explain the different types of savings accounts",
  "context": {
    "customerId": "CUST-12345",
    "purposeCode": "CUSTOMER_SUPPORT",
    "hasConsent": true
  }
}
```

**Response (SSE):**

```
event: message
data: {"content": "There are several types of ", "done": false}

event: message
data: {"content": "savings accounts available:", "done": false}

event: message
data: {"content": "\n\n1. **Easy Access Savings**", "done": false}

event: done
data: {"sessionId": "sess-def456", "tokenCount": 245, "latencyMs": 3421}
```

**SSE Event Types:**

| Event | Description |
|-------|-------------|
| `message` | Content chunk |
| `done` | Stream complete with metadata |
| `error` | Error occurred |

---

## Admin API

### GET /api/admin/kill-switch/status

Get current kill switch status.

**Request:**

```http
GET /api/admin/kill-switch/status HTTP/1.1
Host: agent.regulus.internal
Authorization: Bearer <admin-token>
```

**Response:**

```json
{
  "global": false,
  "scopes": {
    "customer-support-agent": {
      "active": true,
      "activatedAt": "2024-01-15T09:00:00Z",
      "reason": "Maintenance window",
      "activators": ["john.smith@bank.com", "jane.doe@bank.com"]
    },
    "payment-agent": {
      "active": false
    }
  }
}
```

---

### POST /api/admin/kill-switch/activate

Initiate kill switch activation (requires dual-control approval).

**Request:**

```http
POST /api/admin/kill-switch/activate HTTP/1.1
Host: agent.regulus.internal
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "scope": "customer-support-agent",
  "reason": "Anomalous behavior detected - INC123456",
  "severity": "HIGH"
}
```

**Response:**

```json
{
  "requestId": "ks-req-abc123",
  "status": "PENDING_APPROVAL",
  "initiator": "john.smith@bank.com",
  "scope": "customer-support-agent",
  "reason": "Anomalous behavior detected - INC123456",
  "expiresAt": "2024-01-15T10:35:00Z"
}
```

---

### POST /api/admin/kill-switch/approve/{requestId}

Approve a pending kill switch activation.

**Request:**

```http
POST /api/admin/kill-switch/approve/ks-req-abc123 HTTP/1.1
Host: agent.regulus.internal
Authorization: Bearer <admin-token>
```

**Response:**

```json
{
  "requestId": "ks-req-abc123",
  "status": "ACTIVATED",
  "activatedAt": "2024-01-15T10:32:15Z",
  "activators": ["john.smith@bank.com", "jane.doe@bank.com"],
  "scope": "customer-support-agent"
}
```

---

### POST /api/admin/kill-switch/deactivate

Initiate kill switch deactivation.

**Request:**

```http
POST /api/admin/kill-switch/deactivate HTTP/1.1
Host: agent.regulus.internal
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "scope": "customer-support-agent",
  "reason": "Issue resolved - fix deployed in v1.2.3"
}
```

---

## Health Endpoints

### GET /actuator/health

Overall application health.

**Response:**

```json
{
  "status": "UP",
  "components": {
    "llm": {
      "status": "UP",
      "details": {
        "provider": "gemini",
        "model": "gemini-2.0-flash"
      }
    },
    "redis": {
      "status": "UP"
    },
    "db": {
      "status": "UP"
    },
    "killSwitch": {
      "status": "UP",
      "details": {
        "active": false
      }
    }
  }
}
```

---

### GET /actuator/health/liveness

Kubernetes liveness probe.

**Response:**

```json
{
  "status": "UP"
}
```

---

### GET /actuator/health/readiness

Kubernetes readiness probe.

**Response:**

```json
{
  "status": "UP"
}
```

---

## Metrics Endpoint

### GET /actuator/prometheus

Prometheus metrics export.

**Response:**

```
# HELP regulus_llm_requests_total Total LLM requests
# TYPE regulus_llm_requests_total counter
regulus_llm_requests_total{provider="gemini",model="gemini-2.0-flash"} 1234

# HELP regulus_llm_latency_seconds LLM request latency
# TYPE regulus_llm_latency_seconds histogram
regulus_llm_latency_seconds_bucket{le="0.1"} 100
regulus_llm_latency_seconds_bucket{le="0.5"} 450
regulus_llm_latency_seconds_bucket{le="1.0"} 890
regulus_llm_latency_seconds_bucket{le="+Inf"} 1234

# HELP regulus_policy_violations_total Policy violations
# TYPE regulus_policy_violations_total counter
regulus_policy_violations_total{type="MISSING_CONSENT"} 12
regulus_policy_violations_total{type="INVALID_PURPOSE"} 5
```

---

## Audit Endpoints

### GET /api/audit/events

Query audit events.

**Request:**

```http
GET /api/audit/events?sessionId=sess-abc123&from=2024-01-14T00:00:00Z&to=2024-01-15T00:00:00Z HTTP/1.1
Host: agent.regulus.internal
Authorization: Bearer <admin-token>
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `sessionId` | string | No | Filter by session |
| `userId` | string | No | Filter by user |
| `eventType` | string | No | Filter by event type |
| `from` | datetime | Yes | Start time (ISO 8601) |
| `to` | datetime | Yes | End time (ISO 8601) |
| `limit` | integer | No | Max results (default 100) |

**Response:**

```json
{
  "events": [
    {
      "eventId": "evt-123",
      "eventType": "LLM_REQUEST",
      "timestamp": "2024-01-15T10:30:00Z",
      "sessionId": "sess-abc123",
      "userId": "user-456",
      "details": {
        "provider": "gemini",
        "model": "gemini-2.0-flash",
        "inputTokens": 50,
        "outputTokens": 100,
        "latencyMs": 892
      }
    }
  ],
  "totalCount": 1,
  "hasMore": false
}
```

---

## Error Response Format

All errors follow a consistent format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "details": {
      "field": "fieldName",
      "constraint": "constraintType",
      "value": "providedValue"
    }
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req-xyz789",
  "path": "/api/v1/agent/chat"
}
```
