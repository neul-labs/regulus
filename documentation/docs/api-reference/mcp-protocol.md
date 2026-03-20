# MCP Protocol Reference

Model Context Protocol JSON-RPC 2.0 API reference.

## Overview

MCP uses JSON-RPC 2.0 over various transports (stdio, HTTP, WebSocket). This reference covers the protocol messages and tool definitions.

## Connection

### HTTP Transport

```
POST /mcp HTTP/1.1
Host: agent.regulus.internal:8081
Content-Type: application/json
```

### WebSocket Transport

```
ws://agent.regulus.internal:8081/mcp/ws
```

## Protocol Messages

### Initialize

Initialize the MCP session.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {}
    },
    "clientInfo": {
      "name": "my-client",
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
      "tools": {
        "listChanged": true
      }
    },
    "serverInfo": {
      "name": "regulus-mcp-server",
      "version": "0.1.0"
    }
  }
}
```

---

### List Tools

Get available tools.

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
        "name": "get_account_balance",
        "description": "Get the current balance for a customer account",
        "inputSchema": {
          "type": "object",
          "properties": {
            "accountId": {
              "type": "string",
              "description": "The account ID (8 characters)"
            }
          },
          "required": ["accountId"]
        }
      },
      {
        "name": "get_transactions",
        "description": "Get recent transactions for an account",
        "inputSchema": {
          "type": "object",
          "properties": {
            "accountId": {
              "type": "string",
              "description": "The account ID"
            },
            "limit": {
              "type": "integer",
              "description": "Number of transactions to return",
              "default": 10
            },
            "fromDate": {
              "type": "string",
              "format": "date",
              "description": "Start date (ISO 8601)"
            }
          },
          "required": ["accountId"]
        }
      }
    ]
  }
}
```

---

### Call Tool

Invoke a tool.

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "get_account_balance",
    "arguments": {
      "accountId": "12345678"
    }
  }
}
```

**Response (Success):**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"accountId\":\"12345678\",\"balance\":1234.56,\"currency\":\"GBP\",\"asOf\":\"2024-01-15T10:30:00Z\"}"
      }
    ],
    "isError": false
  }
}
```

**Response (Error):**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Account not found: 12345678"
      }
    ],
    "isError": true
  }
}
```

---

## Tool Definitions

### get_account_balance

Get the current balance for a customer account.

**Input Schema:**

```json
{
  "type": "object",
  "properties": {
    "accountId": {
      "type": "string",
      "description": "The account ID (8 alphanumeric characters)",
      "pattern": "^[A-Z0-9]{8}$"
    }
  },
  "required": ["accountId"]
}
```

**Output:**

```json
{
  "accountId": "12345678",
  "balance": 1234.56,
  "currency": "GBP",
  "availableBalance": 1200.00,
  "asOf": "2024-01-15T10:30:00Z"
}
```

---

### get_transactions

Get recent transactions for an account.

**Input Schema:**

```json
{
  "type": "object",
  "properties": {
    "accountId": {
      "type": "string",
      "description": "The account ID"
    },
    "limit": {
      "type": "integer",
      "description": "Number of transactions to return",
      "minimum": 1,
      "maximum": 100,
      "default": 10
    },
    "fromDate": {
      "type": "string",
      "format": "date",
      "description": "Start date for transactions"
    },
    "toDate": {
      "type": "string",
      "format": "date",
      "description": "End date for transactions"
    }
  },
  "required": ["accountId"]
}
```

**Output:**

```json
{
  "accountId": "12345678",
  "transactions": [
    {
      "id": "txn-001",
      "date": "2024-01-15",
      "description": "TESCO STORES",
      "amount": -45.67,
      "currency": "GBP",
      "balance": 1234.56,
      "category": "GROCERIES"
    }
  ],
  "totalCount": 1,
  "hasMore": false
}
```

---

### validate_sort_code

Validate a UK sort code.

**Input Schema:**

```json
{
  "type": "object",
  "properties": {
    "sortCode": {
      "type": "string",
      "description": "Sort code (format: 12-34-56 or 123456)",
      "pattern": "^\\d{2}-?\\d{2}-?\\d{2}$"
    }
  },
  "required": ["sortCode"]
}
```

**Output:**

```json
{
  "sortCode": "12-34-56",
  "valid": true,
  "bankName": "Example Bank PLC",
  "branchName": "London Branch",
  "bic": "EXBKGB2L"
}
```

---

## Error Codes

MCP error codes follow JSON-RPC 2.0:

| Code | Meaning |
|------|---------|
| -32700 | Parse error |
| -32600 | Invalid request |
| -32601 | Method not found |
| -32602 | Invalid params |
| -32603 | Internal error |

Custom error codes:

| Code | Meaning |
|------|---------|
| -32001 | Tool not found |
| -32002 | Tool execution failed |
| -32003 | Unauthorized |
| -32004 | Rate limited |

**Error Response:**

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "error": {
    "code": -32602,
    "message": "Invalid params",
    "data": {
      "field": "accountId",
      "reason": "Must be 8 alphanumeric characters"
    }
  }
}
```

## Notifications

### tools/list_changed

Sent when available tools change.

```json
{
  "jsonrpc": "2.0",
  "method": "notifications/tools/list_changed"
}
```

## Security

### Authentication

Include authentication in the initialize request:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {
      "name": "my-client",
      "version": "1.0.0"
    },
    "authentication": {
      "type": "bearer",
      "token": "your-jwt-token"
    }
  }
}
```

### Rate Limiting

Rate limits apply per client:

- 100 tool calls per minute
- 10 list operations per minute

Rate limit headers (HTTP transport):

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705316400
```
