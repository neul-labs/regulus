# API Reference

Technical reference documentation for Regulus APIs.

## Overview

Regulus exposes several APIs for agent interaction and administration:

| API | Purpose | Protocol |
|-----|---------|----------|
| **Agent Chat API** | Agent interactions | REST/HTTP |
| **Admin API** | Kill switch, configuration | REST/HTTP |
| **MCP Server** | Tool exposure | JSON-RPC 2.0 |
| **A2A Server** | Cross-agent communication | JSON-RPC 2.0 |
| **Metrics** | Prometheus metrics | HTTP |
| **Health** | Health checks | HTTP |

## Base URLs

| Environment | URL |
|------------|-----|
| Local | `http://localhost:8080` |
| Development | `https://agent-dev.regulus.internal` |
| Production | `https://agent.regulus.internal` |

## Authentication

### JWT Authentication

All API endpoints require JWT authentication:

```bash
curl -X POST https://agent.regulus.internal/api/v1/agent/chat \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello"}'
```

### Obtaining Tokens

Tokens are obtained from your identity provider (Azure AD, Okta, etc.):

```bash
# Azure AD example
TOKEN=$(curl -X POST \
  "https://login.microsoftonline.com/$TENANT_ID/oauth2/v2.0/token" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "scope=api://$API_ID/.default" \
  -d "grant_type=client_credentials" | jq -r '.access_token')
```

## API Sections

### [REST Endpoints](rest-endpoints.md)

Core REST API for agent interactions:

- Chat API
- Streaming API
- Admin API
- Health endpoints

### [MCP Protocol](mcp-protocol.md)

Model Context Protocol for tool exposure:

- Tool registration
- Tool invocation
- Schema definitions

### [Configuration](configuration.md)

Configuration reference:

- Application properties
- Environment variables
- Spring profiles

## Common Response Formats

### Success Response

```json
{
  "content": "Response content here",
  "sessionId": "sess-abc123",
  "timestamp": "2024-01-15T10:30:00Z",
  "metadata": {
    "modelId": "gemini-2.0-flash",
    "tokenCount": 150,
    "latencyMs": 1234
  }
}
```

### Error Response

```json
{
  "error": {
    "code": "POLICY_VIOLATION",
    "message": "Purpose code required",
    "details": {
      "field": "purposeCode",
      "constraint": "required"
    }
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req-xyz789"
}
```

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid request parameters |
| `UNAUTHORIZED` | 401 | Missing or invalid authentication |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `POLICY_VIOLATION` | 403 | Request violates policy |
| `NOT_FOUND` | 404 | Resource not found |
| `RATE_LIMITED` | 429 | Rate limit exceeded |
| `SERVICE_UNAVAILABLE` | 503 | Kill switch active or service down |
| `LLM_ERROR` | 502 | Upstream LLM error |

## Rate Limits

| Endpoint | Limit | Window |
|----------|-------|--------|
| `/api/v1/agent/chat` | 100 | per minute |
| `/api/v1/agent/stream` | 50 | per minute |
| `/api/admin/*` | 10 | per minute |

Rate limit headers:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705316400
```

## Versioning

APIs are versioned via URL path:

- `/api/v1/...` - Current stable version
- `/api/v2/...` - Next version (when available)

Breaking changes increment the major version.
