# Regulus Platform Demo - AI Agent Governance

This demo showcases the core Regulus platform components for building compliant AI agents in regulated financial services.

## Quick Start

```bash
./gradlew :examples:agent-demo:run
```

## What This Demo Shows

The demo demonstrates **actual Regulus platform components** working together:

### 1. LLM Client (`regulus-ai-llm`)

Provider-agnostic LLM interface supporting multiple backends:

```java
LlmClient client = FakeLlmClient.withDefaultScript();

LlmRequest request = LlmRequest.builder()
    .prompt("What is my account balance?")
    .systemPrompt("You are a banking assistant.")
    .build();

LlmResponse response = client.generate(request);
```

**Capabilities:**
- Gemini, OpenAI, Anthropic, Azure OpenAI providers
- Function/tool calling support
- Streaming responses
- Token usage tracking

### 2. Policy Enforcement (`regulus-ai-policy`)

GDPR-compliant policy guards that evaluate requests before execution:

```java
List<PolicyGuard> guards = List.of(
    new ConsentPolicyGuard(),
    new PurposeCodePolicyGuard()
);
PolicyEnforcer enforcer = new PolicyEnforcer(guards);

PolicyContext context = PolicyContext.builder()
    .correlationId("demo-001")
    .userId("customer-123")
    .consentGranted(true)
    .lawfulBasis("CONSENT")
    .purposeCode("CONSENT")
    .build();

PolicyResult result = enforcer.enforceAll(context);
if (result.isDenied()) {
    // Handle policy violation
    for (var violation : result.getViolations()) {
        System.out.println(violation.policyName() + ": " + violation.message());
    }
}
```

**Available Guards:**
- `ConsentPolicyGuard` - GDPR Article 6 consent requirements
- `PurposeCodePolicyGuard` - Valid purpose code enforcement
- `LeiPolicyGuard` - Legal Entity Identifier validation

**Valid Purpose Codes:**
- `CONSENT` - Processing based on explicit consent
- `CONTRACT_PERFORMANCE` - Contract fulfilment
- `LEGAL_OBLIGATION` - Regulatory compliance
- `LEGITIMATE_INTEREST` - Business interest processing
- `CUSTOMER_SERVICE` - Customer support activities
- `PAYMENT_PROCESSING` - Payment operations
- `FRAUD_PREVENTION` - Fraud detection
- `AML_KYC` - Anti-money laundering
- `REGULATORY_REPORTING` - Regulatory submissions

### 3. Privacy Filter (`regulus-ai-privacy`)

PII detection and redaction for UK financial services:

```java
PiiPatternFilter filter = new PiiPatternFilter();

String sensitiveText = "Customer NI: AB123456C, account 12345678";
RedactionResult result = filter.redact(sensitiveText);

System.out.println(result.redactedContent());
// Output: Customer NI: [NINO:redacted], account [ACCT:********]
```

**Detected PII Types:**
| Pattern | Example | Redacted As |
|---------|---------|-------------|
| UK National Insurance | AB123456C | `[NINO:redacted]` |
| UK Sort Code | 12-34-56 | `[SORT:**-**-**]` |
| UK Account Number | 12345678 | `[ACCT:********]` |
| Credit Card | 4111-1111-1111-1111 | `[CARD:****-****-****-****]` |
| UK Phone | 07700900123 | `[PHONE:redacted]` |
| Email | john@example.com | `[EMAIL:redacted]` |
| UK Postcode | SW1A 2AA | `[POST:redacted]` |
| Date of Birth | 15/03/1985 | `[DOB:**/**/****]` |
| IBAN | GB82WEST12345698765432 | `[IBAN:redacted]` |
| BIC/SWIFT | WESTGB2L | `[BIC:redacted]` |

### 4. Audit Logging (`regulus-ai-observability`)

Compliance-grade logging for FCA SYSC 9 record keeping:

```java
AuditLogger auditLogger = new AuditLogger();

// Log LLM calls
auditLogger.logLlmCall(
    correlationId, userId, model, provider,
    inputTokens, outputTokens, durationMs, success
);

// Log policy violations
auditLogger.logPolicyViolation(
    correlationId, userId, policyName, violationType, message
);

// Log tool executions
auditLogger.log(AuditEvent.builder()
    .type(AuditEvent.EventType.TOOL_EXECUTION)
    .correlationId(correlationId)
    .userId(userId)
    .operation("get_account_balance")
    .outcome(AuditEvent.Outcome.SUCCESS)
    .details(Map.of("accountId", "ACC-12345"))
    .build());
```

**Event Types:**
- `LLM_CALL` - LLM invocations with token counts
- `TOOL_EXECUTION` - Tool/function calls
- `POLICY_VIOLATION` - Blocked requests
- `PII_DETECTED` - Privacy filter findings

### 5. Full Integration

The demo shows how all components work together in a governed agent flow:

```
User Query → Privacy Filter (redact input PII)
           → LLM Processing (generate response)
           → Policy Enforcer (check consent/purpose)
           → Tool Execution (if policies pass)
           → Privacy Filter (redact output PII)
           → Audit Logger (compliance trail)
           → Response
```

## Demo Output

```
══════════════════════════════════════════════════════════════════════
 5. GOVERNED AGENT (Full Integration)
══════════════════════════════════════════════════════════════════════

  User query: "What's my balance for account 12345678?"

  Step 1: Privacy filter on input
    Filtered: "What's my balance for account [ACCT:********]?"
    PII found: 1

  Step 2: LLM processing
    LLM response: TOOL_CALLS
    Tool requested: get_account_balance

  Step 3: Policy enforcement
    Policy check: PASSED

  Step 4: Tool execution
    Raw result: "Account 12345678 balance: £2,450.00"
    Filtered: "Account [ACCT:********] balance: £2,450.00"

  Step 5: Audit logging
    Logged: TOOL_EXECUTION event
```

## Project Structure

```
examples/agent-demo/
├── build.gradle.kts          # Dependencies on platform modules
├── README.md                 # This file
└── src/main/java/com/regulus/demo/
    ├── AgentDemo.java        # Main demo entry point
    ├── FakeLlmClient.java    # Test LLM implementation
    ├── Agent.java            # Simple agent loop
    ├── Tool.java             # Tool interface
    └── BankingTools.java     # Sample banking tools
```

## Dependencies

This demo uses actual Regulus platform modules:

```kotlin
dependencies {
    implementation(project(":platform:core:regulus-ai-llm"))
    implementation(project(":platform:core:regulus-ai-policy"))
    implementation(project(":platform:core:regulus-ai-privacy"))
    implementation(project(":platform:core:regulus-ai-kill-switch"))
    implementation(project(":platform:core:regulus-ai-observability"))
}
```

## Production Usage

For production deployments, enable Spring Boot auto-configuration:

```java
@SpringBootApplication
@EnableAiAgents
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

Configure in `application.yaml`:

```yaml
regulus:
  ai:
    llm:
      provider: openai
      model: gpt-4
    policy:
      enabled-guards:
        - require.Consent
        - require.PurposeCode
        - require.LEI
    privacy:
      pii-detection: enabled
      redaction-mode: replace
```

## Regulatory Context

This platform supports compliance with:

- **UK GDPR** - Data protection and consent management
- **FCA Consumer Duty (PS22/9)** - Customer outcome obligations
- **FCA SYSC 9** - Record keeping requirements
- **PSD2** - Payment services regulation
- **ISO 20022** - Financial messaging standards

## See Also

- [Architecture Documentation](../../docs/architecture/architecture.md)
- [Policy Guards Reference](../../platform/core/regulus-ai-policy/README.md)
- [Privacy Filters Reference](../../platform/core/regulus-ai-privacy/README.md)
