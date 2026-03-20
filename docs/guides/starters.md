# Spring Boot Starters

Regulus ships opinionated starters that make AI agent adoption as simple as adding a dependency. Every starter shares common features: Spring Boot auto-configuration, Micrometer and OpenTelemetry instrumentation, configurable retries/circuit breakers, and audit logging hooks.

## Available Starters

| Starter | Purpose | Key Features |
|---------|---------|--------------|
| `regulus-ai-agents-spring-boot-starter` | Core agent capabilities | ADK integration, MCP/A2A protocols, tool registration |
| `regulus-ai-governance-starter` | SS1/23 compliance | Model registry, artefact generation, approval workflows |
| `regulus-ai-payments-starter` | Payment processing | ISO 20022, CHAPS, Open Banking connectors |
| `regulus-ai-safety-starter` | Safety & resilience | Kill switch, dual-control, data residency, PII redaction |
| `regulus-ai-evals-client-starter` | Quality gates | Eval service integration, red-team testing |

**Artifact Coordinates**: Group ID `com.regulus.platform`

---

## regulus-ai-agents-spring-boot-starter

Enables ADK agents, planners, memory providers, and tool registration with full MCP/A2A protocol support.

### Features

- **Agent Annotations**: `@EnableAiAgents`, `@Agent`, `@Tool`
- **MCP Server**: JSON-RPC 2.0 tool exposure with resources and prompts protocols
- **MCP Client**: Remote tool discovery and invocation
- **A2A Protocol**: Agent-to-agent communication with task streaming
- **LLM Streaming**: Real-time token streaming for Gemini, OpenAI, Anthropic
- **GCP Authentication**: Native ADC and service account support

### Configuration

```yaml
regulus:
  ai:
    agents:
      enabled: true
    mcp:
      server:
        enabled: true
        path: /mcp
      streaming:
        enabled: true
    a2a:
      server:
        enabled: true
        path: /a2a
      streaming:
        enabled: true
    llm:
      provider: gemini  # gemini | openai | anthropic
      streaming:
        enabled: true
      gemini:
        project-id: ${GCP_PROJECT_ID}
        location: europe-west2  # UK region
        model: gemini-1.5-pro
      openai:
        api-key: ${OPENAI_API_KEY}
        model: gpt-4o
      anthropic:
        api-key: ${ANTHROPIC_API_KEY}
        model: claude-sonnet-4-20250514
    gcp:
      authentication:
        mode: APPLICATION_DEFAULT  # APPLICATION_DEFAULT | SERVICE_ACCOUNT_FILE | METADATA_SERVER
        service-account-file: ${GCP_SA_KEY_PATH:}
```

---

## regulus-ai-governance-starter

Supplies [PRA SS1/23](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss) artefact generation, approval workflow integration, lineage tracking, and review cadence enforcement.

### Features

- **Model Registry**: SS1/23 compliant model inventory with risk tiering
- **Policy Annotations**: `@RequireLEI`, `@RequirePurposeCode`, `@RequireConsent`
- **Artefact Generation**: Model cards, validation reports, challenger comparisons
- **Approval Workflows**: Integration with ServiceNow/GRC systems

### Configuration

```yaml
regulus:
  ai:
    governance:
      enabled: true
      model-registry:
        enabled: true
        sync-to-inventory: true
        inventory-endpoint: ${MODEL_INVENTORY_URL}
      policies:
        enforced:
          - require.LEI
          - require.PurposeCode
          - require.Consent
      artefacts:
        output-dir: ./governance-artefacts
        auto-generate: true
```

---

## regulus-ai-payments-starter

Adds ISO 20022/CHAPS validators, Open Banking connectors, LEI/purpose checks, and payment-specific telemetry.

### Features

- **ISO 20022 Validation**: PAIN, PACS, CAMT message validation
- **CHAPS Integration**: Real-time gross settlement support
- **Open Banking**: PSD2 compliant API connectors
- **Payment Telemetry**: Transaction tracing and audit

### Configuration

```yaml
regulus:
  ai:
    payments:
      enabled: true
      iso20022:
        validation: strict  # strict | lenient
        schemas:
          - pain.001.001.09
          - pacs.008.001.08
      chaps:
        enabled: true
        endpoint: ${CHAPS_GATEWAY_URL}
      open-banking:
        enabled: true
        aspsp-endpoint: ${ASPSP_URL}
```

---

## regulus-ai-safety-starter

Delivers comprehensive safety controls for UK financial services: kill switch with dual-control (4-eyes principle), data residency enforcement for UK GDPR/FCA compliance, PII redaction, and prompt injection detection.

### Features

- **Kill Switch**: Global, connector, and tool-level disablement
- **Dual-Control (4-Eyes)**: Requires two authorised approvers for critical operations
- **Data Residency**: Enforces UK/EU data processing requirements
- **PII Redaction**: Pattern-based and JSONPath redaction
- **Prompt Injection Detection**: Rule-based attack detection

### Kill Switch Configuration

```yaml
regulus:
  ai:
    safety:
      enabled: true
      kill-switch:
        enabled: true
        provider: in-memory  # in-memory | config-hub | vault
        dual-control:
          enabled: true
          required-approvers: 2
          allow-emergency-bypass: true
          allow-self-approval: false
          authorized-approvers:
            - risk-team@bank.com
            - ai-ops@bank.com
            - model-risk@bank.com
```

### Data Residency Configuration

```yaml
regulus:
  ai:
    safety:
      data-residency:
        enabled: true
        # UK-approved regions for regulated data processing
        allowed-regions:
          - europe-west2      # GCP London
          - europe-west1      # GCP Belgium (EU adequacy)
          - eu-west-2         # AWS London
          - uksouth           # Azure UK South
          - ukwest            # Azure UK West
        enforce-uk-residency: true  # UK_REGULATED data must stay in UK
        block-violations: true      # Block requests to non-approved regions
        allow-unknown-regions: false
```

### Privacy & PII Configuration

```yaml
regulus:
  ai:
    safety:
      privacy:
        pii-pattern:
          enabled: true
        json-path:
          enabled: true
          paths:
            - $.password
            - $.secret
            - $.apiKey
            - $.creditCard
            - $.ssn
            - $.nationalInsuranceNumber
            - $.sortCode
            - $.accountNumber
      prompt-injection:
        enabled: true
        block-on-detection: true
```

### Data Classification Levels

The data residency enforcer supports these classification levels aligned with UK financial services standards:

| Classification | Description | Region Restrictions |
|----------------|-------------|---------------------|
| `PUBLIC` | No restrictions | Any region |
| `STANDARD` | Standard business data | Allowed regions only |
| `INTERNAL` | Internal only | Allowed regions only |
| `CONFIDENTIAL` | Confidential business data | Allowed regions only |
| `PII` | Personally Identifiable Information | UK/EEA, transfers need approval |
| `SENSITIVE` | Special category data (GDPR Art 9) | UK/EEA, transfers need approval |
| `UK_REGULATED` | FCA/PRA regulated data | UK only (no transfers) |
| `CRITICAL` | Systemically important data | UK only (no transfers) |

---

## regulus-ai-evals-client-starter

Connects to the Python-based eval/red-team service for quality gating.

### Features

- **Quality Gates**: `@AiGate` annotation for automated thresholds
- **Eval Service Integration**: Faithfulness, toxicity, relevance metrics
- **Red-Team Testing**: Adversarial prompt testing

### Configuration

```yaml
regulus:
  ai:
    evals:
      enabled: true
      endpoint: http://evals:8080
      gates:
        min-faithfulness: 0.8
        max-toxicity: 0.1
        min-relevance: 0.7
      red-team:
        enabled: true
        scenarios:
          - prompt-injection
          - jailbreak
          - data-exfiltration
```

---

## Complete UK Financial Services Configuration

Here's a complete configuration example for a UK-regulated AI agent deployment:

```yaml
regulus:
  ai:
    # Core agent setup
    agents:
      enabled: true

    # LLM configuration - UK region
    llm:
      provider: gemini
      streaming:
        enabled: true
      gemini:
        project-id: ${GCP_PROJECT_ID}
        location: europe-west2  # GCP London - UK data residency
        model: gemini-1.5-pro

    # GCP authentication
    gcp:
      authentication:
        mode: APPLICATION_DEFAULT

    # MCP server for tool exposure
    mcp:
      server:
        enabled: true
        path: /mcp
      streaming:
        enabled: true

    # A2A for inter-agent communication
    a2a:
      server:
        enabled: true
        path: /a2a
      streaming:
        enabled: true

    # Governance - SS1/23 compliance
    governance:
      enabled: true
      model-registry:
        enabled: true
        sync-to-inventory: true
      policies:
        enforced:
          - require.LEI
          - require.PurposeCode
          - require.Consent

    # Safety controls
    safety:
      enabled: true

      # Kill switch with dual-control
      kill-switch:
        enabled: true
        provider: vault  # Production: use Vault or ConfigHub
        dual-control:
          enabled: true
          required-approvers: 2
          allow-emergency-bypass: true
          allow-self-approval: false
          authorized-approvers:
            - risk-team@bank.com
            - ai-ops@bank.com

      # Data residency - UK GDPR/FCA compliance
      data-residency:
        enabled: true
        allowed-regions:
          - europe-west2  # GCP London
          - eu-west-2     # AWS London
          - uksouth       # Azure UK South
        enforce-uk-residency: true
        block-violations: true
        allow-unknown-regions: false

      # Privacy controls
      privacy:
        pii-pattern:
          enabled: true
        json-path:
          enabled: true
          paths:
            - $.password
            - $.nationalInsuranceNumber
            - $.sortCode
            - $.accountNumber

      # Prompt injection protection
      prompt-injection:
        enabled: true
        block-on-detection: true

    # Evaluation gates
    evals:
      enabled: true
      endpoint: http://evals:8080
      gates:
        min-faithfulness: 0.8
        max-toxicity: 0.1
```

---

## Version Management

- Use the `regulus-ai-bom` Gradle platform dependency to keep starter versions aligned:

```kotlin
// build.gradle.kts
dependencies {
    implementation(platform("com.regulus.platform:regulus-ai-bom:1.0.0"))
    implementation("com.regulus.platform:regulus-ai-agents-spring-boot-starter")
    implementation("com.regulus.platform:regulus-ai-safety-starter")
    implementation("com.regulus.platform:regulus-ai-governance-starter")
}
```

- The Spring Initializr blueprint scaffolds a project with the correct dependencies
- Starters publish change logs detailing new policy rules, validator updates, and configuration additions

---

## Extending Starters

Teams can create custom starters that depend on the core ones:

```kotlin
// my-domain-starter/build.gradle.kts
dependencies {
    api("com.regulus.platform:regulus-ai-agents-spring-boot-starter")
    api("com.regulus.platform:regulus-ai-safety-starter")
    // Add domain-specific dependencies
}
```

Auto-configuration respects Spring profiles for environment-specific behaviour:

```yaml
# application-local.yaml - Development overrides
regulus:
  ai:
    safety:
      kill-switch:
        provider: in-memory
        dual-control:
          enabled: false  # Disable for local dev
      data-residency:
        enabled: false    # Disable for local dev
```

---

## Related Documentation

- [Kill Switch Design](../governance/kill-switch.md) - Detailed kill switch architecture
- [Data Residency Guide](./data-residency.md) - UK GDPR/FCA data residency compliance
- [Model Registry](../governance/model-registry.md) - SS1/23 model inventory
- [ADK/MCP/A2A Integration](../architecture/adk-mcp-a2a.md) - Protocol details
- [Quickstart Tutorial](./quickstart-tutorial.md) - Complete working example
