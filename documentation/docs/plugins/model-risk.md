# RegulusModelRiskPlugin

## In one sentence

Rejects model and code-executor invocations that exceed the tenant's
approved risk tier; covers EU AI Act risk classification and PRA SS1/23
model materiality in one mechanism.

## Who does it apply to?

EU AI Act-bound deployers (the high-risk classification depends on Annex
III scope) and PRA-authorised firms (SS1/23 model risk). The plugin is
useful even outside regulated contexts as a guardrail against accidental
escalation to a more expensive / more capable model.

## The two-minute explainer

Two tiering schemes mapped to one Regulus enum:

| Regulus tier | EU AI Act analogy | SS1/23 analogy |
|---|---|---|
| `EXPERIMENTAL` | Minimal-risk / internal | Below Tier 3 |
| `STANDARD` | Limited-risk | Tier 3 |
| `REGULATED` | Adjacent to high-risk (regulated use) | Tier 2 |
| `HIGH_RISK` | High-risk (Annex III) | Tier 1 |

The plugin checks two things on every model and tool call:

- The model ID's tier from the `ModelRegistry` ≤ the tenant's allowed tier.
- The tool ID (if a tool call) is not in the high-risk list. ADK's
  `ContainerCodeExecutor` and `VertexAiCodeExecutor` are flagged HIGH_RISK
  by default — they can execute arbitrary code on the model's behalf.

A tier-exceeding call short-circuits at `BeforeModelCallback` or
`BeforeToolCallback` and is logged.

## What it actually requires of an engineer

- Decide the tenant's tier ceiling.
- Maintain the model registry — at minimum, classify the models you use.
- For high-risk code executors, escalate via an explicit policy decision
  (you have to deliberately allow them).

## What Regulus does for you

- Default registry covers common Gemini / OpenAI / Anthropic models with
  conservative tiers.
- Codes the EU AI Act / SS1/23 cross-walk so you don't have to.
- Marks code executors as high-risk by default — the safe-by-default
  position.

## Saves you ~

- ~5 engineer-weeks for the registry + per-tenant policy + audit linkage
  + auditor evidence pack.

## Code: minimal

```java
RegulusModelRiskPlugin riskPlugin = RegulusModelRiskPlugin.tier(Tier.STANDARD);
```

## Code: production

```java
ModelRegistry registry = ModelRegistry.of(Map.ofEntries(
    Map.entry("gemini-2.5-flash", Tier.STANDARD),
    Map.entry("gemini-2.5-pro",   Tier.REGULATED),
    Map.entry("gemini-1.5-pro",   Tier.REGULATED),
    Map.entry("gpt-4o",           Tier.REGULATED),
    Map.entry("claude-opus-4-7",  Tier.REGULATED),
    Map.entry("internal-fine-tuned-decisioner-v2", Tier.HIGH_RISK)
));

RegulusModelRiskPlugin riskPlugin = RegulusModelRiskPlugin.tier(
    Tier.REGULATED, registry);
```

```yaml
regulus:
  adk:
    model-risk:
      tenant-tier: REGULATED
```

## How to verify

- Call `gemini-2.5-pro` from a `STANDARD`-tier tenant → blocked with
  citation `SS1/23 §3` / `AI Act Annex III` (where applicable).
- Attempt to invoke `ContainerCodeExecutor` → blocked unless tenant is
  `HIGH_RISK`.
- Sample event: `model_id`, `model_version`, `model_risk_tier`,
  `validation_status` present.

## What an auditor will ask

1. **"What's your model inventory?"** Registry source of truth.
2. **"How did you classify this model?"** Tier rationale per model;
   linked validation evidence.
3. **"What about the code executors?"** Default high-risk; demonstrate the
   block.

## What this doesn't cover

- **The classification decision itself.** You decide; we enforce.
- **Validation activity.** External process; we audit-link.
- **EU AI Act conformity assessment for high-risk providers.** That's a
  provider obligation, not a deployer one.

## Citations

See [Concepts → Risk tiers](../concepts/risk-tiers.md),
[EU AI Act](../compliance/eu/eu-ai-act.md),
[PRA SS1/23](../compliance/uk/pra-ss1-23.md).
