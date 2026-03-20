# Configuration Reference

Complete configuration reference for Regulus.

## Configuration Sources

Regulus supports configuration from multiple sources (in order of precedence):

1. Command-line arguments
2. Environment variables
3. `application.yml` or `application.properties`
4. Spring Cloud Config Server
5. Default values

## LLM Configuration

### Provider Selection

```yaml
regulus:
  llm:
    provider: gemini  # gemini, openai, anthropic, azure-openai
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.llm.provider` | string | - | LLM provider to use |
| `regulus.llm.timeout` | duration | 30s | Request timeout |
| `regulus.llm.connect-timeout` | duration | 10s | Connection timeout |
| `regulus.llm.max-tokens` | integer | 4096 | Maximum output tokens |

### Google Vertex AI (Gemini)

```yaml
regulus:
  llm:
    provider: gemini
    gemini:
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
      model: gemini-2.0-flash
      temperature: 0.7
      top-p: 0.95
      top-k: 40
      max-output-tokens: 8192
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.llm.gemini.project-id` | string | - | GCP project ID |
| `regulus.llm.gemini.location` | string | us-central1 | GCP region |
| `regulus.llm.gemini.model` | string | gemini-1.5-flash | Model name |
| `regulus.llm.gemini.temperature` | float | 0.7 | Sampling temperature |
| `regulus.llm.gemini.top-p` | float | 0.95 | Nucleus sampling |
| `regulus.llm.gemini.top-k` | integer | 40 | Top-k sampling |

### OpenAI

```yaml
regulus:
  llm:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o
      temperature: 0.7
      max-tokens: 4096
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.llm.openai.api-key` | string | - | OpenAI API key |
| `regulus.llm.openai.model` | string | gpt-4o | Model name |
| `regulus.llm.openai.organization-id` | string | - | Organization ID |
| `regulus.llm.openai.temperature` | float | 0.7 | Sampling temperature |

### Anthropic

```yaml
regulus:
  llm:
    provider: anthropic
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-3-5-sonnet-20241022
      temperature: 0.7
      max-tokens: 4096
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.llm.anthropic.api-key` | string | - | Anthropic API key |
| `regulus.llm.anthropic.model` | string | claude-3-5-sonnet-20241022 | Model name |
| `regulus.llm.anthropic.temperature` | float | 0.7 | Sampling temperature |

### Azure OpenAI

```yaml
regulus:
  llm:
    provider: azure-openai
    azure-openai:
      endpoint: ${AZURE_OPENAI_ENDPOINT}
      api-key: ${AZURE_OPENAI_API_KEY}
      deployment-name: gpt-4o
      api-version: "2024-02-01"
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.llm.azure-openai.endpoint` | string | - | Azure endpoint URL |
| `regulus.llm.azure-openai.api-key` | string | - | API key |
| `regulus.llm.azure-openai.deployment-name` | string | - | Deployment name |
| `regulus.llm.azure-openai.api-version` | string | 2024-02-01 | API version |

## Policy Configuration

```yaml
regulus:
  policy:
    enabled: true
    require-purpose-code: true
    require-consent: true
    allowed-purpose-codes:
      - CUSTOMER_SUPPORT
      - ACCOUNT_INQUIRY
      - FRAUD_DETECTION
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.policy.enabled` | boolean | true | Enable policy enforcement |
| `regulus.policy.require-purpose-code` | boolean | true | Require purpose code |
| `regulus.policy.require-consent` | boolean | true | Require consent flag |
| `regulus.policy.allowed-purpose-codes` | list | [] | Allowed purpose codes |

## Privacy Configuration

```yaml
regulus:
  privacy:
    redaction:
      enabled: true
      replacement: "[REDACTED]"
      patterns:
        nino: true
        sort-code: true
        account-number: true
        credit-card: true
        phone-uk: true
        email: true
        postcode: true
        iban: true
        bic: true
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.privacy.redaction.enabled` | boolean | true | Enable PII redaction |
| `regulus.privacy.redaction.replacement` | string | [REDACTED] | Replacement text |
| `regulus.privacy.redaction.patterns.*` | boolean | true | Enable specific patterns |

## Kill Switch Configuration

```yaml
regulus:
  kill-switch:
    enabled: true
    check-interval: 30s
    backend: redis
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      key-prefix: "regulus:killswitch:"
    dual-control:
      enabled: true
      required-approvers: 2
      approval-timeout: 5m
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.kill-switch.enabled` | boolean | true | Enable kill switch |
| `regulus.kill-switch.check-interval` | duration | 30s | Status check interval |
| `regulus.kill-switch.backend` | string | config | Backend type |
| `regulus.kill-switch.dual-control.enabled` | boolean | true | Require dual control |
| `regulus.kill-switch.dual-control.required-approvers` | integer | 2 | Required approvers |

## Data Residency Configuration

```yaml
regulus:
  data-residency:
    enabled: true
    enforcement: strict
    regions:
      uk:
        allowed: true
        providers:
          gcp: europe-west2
          aws: eu-west-2
          azure: uksouth,ukwest
      eu:
        allowed: true
        adequacy: true
      us:
        allowed: false
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.data-residency.enabled` | boolean | true | Enable enforcement |
| `regulus.data-residency.enforcement` | string | strict | Enforcement mode |
| `regulus.data-residency.regions.*.allowed` | boolean | - | Allow region |

## Model Registry Configuration

```yaml
regulus:
  model-registry:
    enabled: true
    model-id: customer-support-agent-v1
    risk-tier: MEDIUM
    owner: ai-platform-team
    review-cadence: QUARTERLY
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.model-registry.enabled` | boolean | true | Enable registry |
| `regulus.model-registry.model-id` | string | - | Unique model ID |
| `regulus.model-registry.risk-tier` | enum | MEDIUM | Risk tier |
| `regulus.model-registry.review-cadence` | enum | QUARTERLY | Review frequency |

## Audit Configuration

```yaml
regulus:
  audit:
    enabled: true
    destination: kafka
    kafka:
      topic: regulus.audit.events
      bootstrap-servers: ${KAFKA_BROKERS}
    retention-days: 2555
    events:
      llm-calls: true
      policy-violations: true
      pii-detections: true
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.audit.enabled` | boolean | true | Enable audit logging |
| `regulus.audit.destination` | string | kafka | Audit destination |
| `regulus.audit.retention-days` | integer | 2555 | Retention period (days) |

## MCP Configuration

```yaml
regulus:
  mcp:
    server:
      enabled: true
      port: 8081
      transport: http
    client:
      enabled: true
      servers:
        - name: banking-tools
          url: http://localhost:8082/mcp
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.mcp.server.enabled` | boolean | false | Enable MCP server |
| `regulus.mcp.server.port` | integer | 8081 | Server port |
| `regulus.mcp.server.transport` | string | stdio | Transport type |

## A2A Configuration

```yaml
regulus:
  a2a:
    enabled: true
    agent:
      id: customer-support-agent
      name: Customer Support Agent
      capabilities:
        - customer-lookup
        - account-inquiry
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regulus.a2a.enabled` | boolean | false | Enable A2A |
| `regulus.a2a.agent.id` | string | - | Agent identifier |
| `regulus.a2a.agent.name` | string | - | Agent display name |

## Environment Variables

All properties can be set via environment variables:

```bash
# Convert property path to uppercase with underscores
regulus.llm.provider → REGULUS_LLM_PROVIDER
regulus.llm.gemini.project-id → REGULUS_LLM_GEMINI_PROJECT_ID
regulus.kill-switch.enabled → REGULUS_KILL_SWITCH_ENABLED
```

## Spring Profiles

### Development Profile

```yaml
# application-dev.yml
regulus:
  llm:
    provider: openai  # Easier setup for dev
  kill-switch:
    enabled: false  # Disable for development
  policy:
    require-consent: false  # Relaxed for testing

logging:
  level:
    com.regulus: DEBUG
```

### Production Profile

```yaml
# application-prod.yml
regulus:
  llm:
    provider: gemini
    gemini:
      location: europe-west2  # UK data residency
  kill-switch:
    enabled: true
    dual-control:
      enabled: true
  data-residency:
    enforcement: strict

logging:
  level:
    root: INFO
```

Activate profile:

```bash
java -jar app.jar --spring.profiles.active=prod
```

Or:

```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```
