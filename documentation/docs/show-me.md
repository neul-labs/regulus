# Show me — the diff

Two side-by-side code blocks. The audit events each produces. The
GRC envelope the second one emits. Done.

## Plain ADK agent

```java
package com.example.agent;

import com.google.adk.agents.LlmAgent;
import com.google.adk.app.App;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }

    LlmAgent rootAgent() {
        return LlmAgent.builder()
                .name("greeter")
                .model("gemini-2.5-flash")
                .instruction("Greet the user politely.")
                .build();
    }
}
```

Runs. Works. **Produces no audit events. No PII redaction. No policy
guards. No kill switch. No model-risk gating. No residency enforcement.
No evidence flowing to the GRC tool.** If a regulator asked you "show me
what this agent did to customer 42 last Tuesday," the only honest answer
is "I don't know."

## Regulus-on-ADK agent

```java
package com.example.agent;

import com.google.adk.agents.LlmAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
    // The Regulus Spring starter auto-wires six plugins around the
    // LlmAgent based on application.yaml — no extra code needed.
}
```

`application.yaml`:

```yaml
regulus:
  compliance:
    profiles: [eu-ai-act, uk-gdpr, fca-sysc]
  governance:
    frameworks: [nist-ai-rmf, nist-ai-rmf-600-1, iso-42001]
  grc:
    stdout: true
  adk:
    name: greeter
    audit:
      sink: stdout
    kill-switch:
      enabled: true
      dual-control: true
    residency:
      allowed-regions: [europe-west2]
    model-risk:
      tenant-tier: STANDARD
```

Same agent. **Now every invocation goes through policy guards, PII
redaction, model-risk gating, kill-switch check, residency validation,
and audit emission — and the evidence flows to your GRC tool.**

## What you see when a request comes in

Inbound: `POST /chat` with a JWT from your enterprise IdP (Okta /
Auth0 / Keycloak — the included OIDC adapter handles all three; SAML
and mTLS plug in through the same `IdentityAdapter` SPI) and prompt
`"Hello, my NINO is AB123456C, can you confirm?"`.

The Regulus filter validates the JWT through Spring Security, mints a
canonical `Identity` (subject + tenant + jurisdiction + purpose +
roles + lawful basis), and binds it to the request thread before any
plugin runs.

The prompt that reaches the model:

```
Hello, my NINO is <NINO_1>, can you confirm?
```

The audit event (one of several emitted — model-call shown here):

```json
{
  "event_id": "01J6X4ABCDEFG",
  "occurred_at": "2026-05-14T11:23:09.123Z",
  "actor": "user:42",
  "tenant_id": "acme-bank",
  "jurisdiction": "EU_UK",
  "identity_adapter": "oidc",
  "smf_holder": "SMF24:Jane Smith",
  "action": "model-call",
  "result": "allow",
  "model_id": "gemini-2.5-flash",
  "model_version": "2026-05-01",
  "purpose_code": "greeting-test",
  "lawful_basis": "Art. 6(1)(b)",
  "regulation_clause": "UK GDPR Art. 25",
  "framework_control_id": "A.7.3",
  "ai_act_risk_tier": "limited",
  "consumer_duty_outcome": "support",
  "redactions": ["NINO_1"],
  "mechanism": "pii-redaction",
  "chain_index": 1284,
  "prev_event_hash": "9f3e…",
  "event_hash": "1c87…"
}
```

The `tenant_id` / `jurisdiction` / `identity_adapter` fields come from
the canonical `Identity` the IdP adapter minted on the inbound side.
The `chain_index` and `prev_event_hash` / `event_hash` fields appear
when `regulus.ai.observability.audit.integrity.enabled=true` — they
let an auditor run `regulus audit verify ./chain.jsonl` offline and
prove tamper-evidence without a running Regulus stack.

The same event becomes a `GrcEvidenceEnvelope` and lands in your GRC
tool (here, stdout for the demo — in production: ServiceNow IRM, OneTrust
AI Governance, or your tool of choice):

```json
{
  "event_id": "01J6X4ABCDEFG",
  "occurred_at": "2026-05-14T11:23:09.123Z",
  "control_framework_id": "iso-42001",
  "control_id": "A.7.3",
  "compliance_profile_id": "uk-gdpr",
  "regulation_clause": "Art. 25",
  "kind": "CONTROL_TEST",
  "actor": "user:42",
  "result": "pass",
  "audit_event_link": "regulus-audit://01J6X4ABCDEFG"
}
```

Your 2L analyst reads this directly from their GRC tool's
control-evidence view and attests the control. Your 3L auditor pulls the
same event by `subject_id`, six months from now, and reproduces the
walk-through. None of that worked an hour ago.

## What's actually firing

Each colour-coded plugin contributes one or more events per request:

| ADK callback | Regulus plugin | Effect |
|---|---|---|
| `BeforeAgentCallback` | `RegulusDataResidencyPlugin` | Re-validates region pin (defence in depth) |
| `BeforeAgentCallback` | `RegulusKillSwitchPlugin` | Checks kill state per scope |
| `BeforeModelCallback` | `RegulusPolicyPlugin` | Purpose binding, consent, Art. 22 safeguards |
| `BeforeModelCallback` (mutating) | `RegulusPrivacyPlugin` | Replaces PII with stable tokens |
| `BeforeModelCallback` | `RegulusModelRiskPlugin` | Tier ceiling enforcement |
| `AfterModelCallback` | `RegulusPrivacyPlugin` | Re-redacts streamed output |
| `AfterModelCallback` | `RegulusAuditPlugin` | Emits the audit event above |
| (downstream) | `RegulusGovernanceEvidencePlugin` | Builds the GRC envelope, hands to each adapter |

Compose them on the ADK `App` directly if you prefer code over YAML:

```java
App app = App.builder("greeter", rootAgent)
    .plugin(RegulusDataResidencyPlugin.allow("europe-west2"))
    .plugin(RegulusKillSwitchPlugin.dualControl())
    .plugin(RegulusPolicyPlugin.fromProfile(profile))
    .plugin(RegulusPrivacyPlugin.withPatterns(NINO, IBAN, BIC, SORT_CODE).build())
    .plugin(RegulusModelRiskPlugin.tier(Tier.STANDARD))
    .plugin(RegulusAuditPlugin.forProfile(profile).toKafka("audit.regulus.v1").build())
    .plugin(RegulusGovernanceEvidencePlugin.forFramework(framework, adapters, auditSink))
    .build();
```

Both paths produce the same audit shape.

## Run it yourself

```bash
# 60s — install the CLI:
curl -fsSL https://raw.githubusercontent.com/neul-labs/regulus/main/install.sh | sh

# 60s — scaffold a project:
regulus init my-agent \
    --profiles=eu-ai-act,uk-gdpr,fca-sysc \
    --frameworks=nist-ai-rmf,iso-42001 \
    --grc-adapter=stdout

# 5min — run it:
cd my-agent && gradle wrapper && ./gradlew bootRun
```

Hit `POST /chat` with a prompt containing a NINO and watch the events
print.

## Where to read next

- [Why Regulus](why-regulus.md) — the broader story.
- [Install the CLI](getting-started/install-cli.md) — full install paths.
- [ADK quickstart](getting-started/adk-quickstart.md) — without the
  scaffold, manual setup.
- [Plugins overview](plugins/index.md) — what each plugin does in detail.
