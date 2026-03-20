# Regulus AI Platform - Quick Start

Get up and running with the Regulus AI Platform in under 5 minutes.

## Prerequisites

- Java 21+
- Gradle 8.5+ (included via wrapper)

## Start the Application

```bash
# From the repository root
make quickstart
```

Or directly:
```bash
./gradlew :examples:quickstart:bootRun
```

The application starts at `http://localhost:8080`.

## Try the APIs

### 1. Health Check
```bash
curl http://localhost:8080/api/health
```

### 2. Validate ISO 20022 Payment
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d '{
    "message": "<CstmrCdtTrfInitn><GrpHdr><MsgId>MSG001</MsgId></GrpHdr><PmtInf><CdtTrfTxInf/></PmtInf></CstmrCdtTrfInitn>",
    "messageType": "pain.001"
  }'
```

### 3. Calculate Risk Score
```bash
curl -X POST http://localhost:8080/api/risk \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX123",
    "amount": 15000,
    "currency": "GBP",
    "receiverCountry": "RU"
  }'
```

### 4. Redact PII
```bash
curl -X POST http://localhost:8080/api/redact \
  -H "Content-Type: application/json" \
  -d '{"content": "Customer sort code 12-34-56, card 4111-1111-1111-1111"}'
```

### 5. Check Policy Compliance
```bash
curl -X POST http://localhost:8080/api/policy/check \
  -H "Content-Type: application/json" \
  -d '{
    "lei": "529900T8BM49AURSDO55",
    "purposeCode": "SERVICE_DELIVERY",
    "consentId": "CONSENT123"
  }'
```

### 6. Toggle Kill Switch
```bash
# Activate global kill switch
curl -X POST http://localhost:8080/api/admin/killswitch \
  -H "Content-Type: application/json" \
  -d '{"scope": "global", "enabled": true, "reason": "Testing"}'

# Deactivate
curl -X POST http://localhost:8080/api/admin/killswitch \
  -H "Content-Type: application/json" \
  -d '{"scope": "global", "enabled": false}'
```

## What's Included

This quickstart demonstrates:

- **MCP Client/Server**: ISO 20022 validation and risk scoring tools
- **Policy Guards**: LEI validation, purpose code checking, consent verification
- **Privacy Filters**: PII detection and redaction (sort codes, card numbers, etc.)
- **Kill Switch**: Global and scoped circuit breakers
- **Observability**: Metrics and audit logging

## Configuration

See `src/main/resources/application.yaml` for all configuration options.

## Next Steps

1. **Enable A2A**: Set `regulus.ai.a2a.enabled=true` to expose as an A2A agent
2. **Enable Security**: Configure API keys, OAuth, or mTLS
3. **Connect to Real MCP Server**: Set `regulus.ai.mcp.mock=false` and configure server URL
4. **Add Custom Tools**: Extend the MCP server with your own tools
