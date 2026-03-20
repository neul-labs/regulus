# Plugins

Six `BasePlugin` implementations that comprise the Regulus compliance plane.
Each is a pure ADK citizen — no Spring requirement, no AOP magic, no
parallel runtime. They compose into ADK's `App` builder.

## At a glance

| Plugin | What it does | ADK hooks |
|---|---|---|
| [`RegulusPolicyPlugin`](policy.md) | Enforces purpose binding, consent, vulnerable-customer routing, Art. 22 safeguards | `BeforeModelCallback`, `BeforeToolCallback` |
| [`RegulusPrivacyPlugin`](privacy.md) | PII redaction before LLM call, output re-redaction | `BeforeModelCallback` (mutate), `AfterModelCallback` |
| [`RegulusAuditPlugin`](audit.md) | Structured immutable audit events + regulation-aware retention | All `After*` callbacks + `EventCompactor` |
| [`RegulusKillSwitchPlugin`](kill-switch.md) | Dual-control / 4-eyes emergency shutdown | `BeforeAgentCallback` + `ToolConfirmation` |
| [`RegulusModelRiskPlugin`](model-risk.md) | Tier-aware model + code-executor gating | `BeforeModelCallback`, `BeforeToolCallback` |
| [`RegulusDataResidencyPlugin`](data-residency.md) | Region allowlist enforcement, fail-closed at startup | Startup + `BeforeAgentCallback` |

## Composing them

```java
App app = App.builder("my-agent", rootAgent)
    .plugin(RegulusPolicyPlugin.fromProfile(profile))
    .plugin(RegulusPrivacyPlugin.withPatterns(NINO, IBAN, BIC, SORT_CODE).build())
    .plugin(RegulusKillSwitchPlugin.dualControl())
    .plugin(RegulusAuditPlugin.forProfile(profile).toKafka("audit.regulus.v1").build())
    .plugin(RegulusDataResidencyPlugin.allow("europe-west2"))
    .plugin(RegulusModelRiskPlugin.tier(Tier.STANDARD))
    .build();
```

Order matters loosely — Regulus orders its own plugins internally so that
residency fires first (fail-closed at startup), then policy + privacy + model-
risk on the inbound side, then audit on the outbound side. You can mix
Regulus plugins with your own `BasePlugin`s freely; ADK runs them all
through the callback chain in registration order.

## When to skip the Spring starter

Use the [Spring Boot starter](../getting-started/adk-quickstart.md) when:
- You're already on Spring Boot.
- You want YAML configuration.
- You want auto-wired beans you can override one at a time.

Skip it (and wire plugins directly) when:
- You're not on Spring (Quarkus, Micronaut, Helidon, plain `main`).
- You want every plugin's configuration in code, near the agent.
- You're building a library that embeds an ADK agent and want zero
  framework dependency.

The plugin API is the same in both cases.

## Pages

- [`RegulusPolicyPlugin`](policy.md)
- [`RegulusPrivacyPlugin`](privacy.md)
- [`RegulusAuditPlugin`](audit.md)
- [`RegulusKillSwitchPlugin`](kill-switch.md)
- [`RegulusModelRiskPlugin`](model-risk.md)
- [`RegulusDataResidencyPlugin`](data-residency.md)
