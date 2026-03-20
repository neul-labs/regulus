# Risk tiers

Two big rulebooks tier AI systems / models by risk. Regulus implements both
through `RegulusModelRiskPlugin`.

## EU AI Act risk pyramid

The AI Act classifies AI systems into four tiers (from base to top):

1. **Minimal risk** — most AI systems. No specific AI Act obligations beyond
   transparency in some uses.
2. **Limited risk** — e.g. chatbots that interact with humans. Transparency
   obligation (the user knows they're talking to AI).
3. **High risk** — listed in Annex III. Examples: credit-scoring,
   recruitment, education, biometric ID, critical infrastructure. Full
   stack of obligations: risk management (Art. 9), data governance (Art. 10),
   logging (Art. 12), transparency (Art. 13), human oversight (Art. 14),
   accuracy (Art. 15), conformity assessment, post-market monitoring.
4. **Unacceptable risk** — banned outright. Social scoring, manipulative
   subliminal techniques, real-time remote biometric ID in public spaces
   (with narrow exceptions).

> **The Annex III list grows.** The Commission can amend it. Stay current via
> the AI Office.

## PRA SS1/23 model risk tiers

The PRA's model risk management framework tiers models by **materiality** to
the firm — not specifically by AI Act risk. Practitioner-level summary:

- **Tier 1** — high materiality (capital, regulatory, customer-impact
  significant). Strictest validation, monitoring, governance.
- **Tier 2** — material but less so. Lighter but still substantial controls.
- **Tier 3** — limited materiality. Lightest controls.

A model can be Tier 1 under SS1/23 while being only "limited risk" under the
AI Act (or vice versa). Regulus expresses both via a single tenant tier
setting, and lets you pick the stricter of the two for any given model.

## How Regulus expresses this

`RegulusModelRiskPlugin.Tier` has four levels:

| Regulus tier | EU AI Act analogy | SS1/23 analogy |
|---|---|---|
| `EXPERIMENTAL` | Minimal risk, dev / internal | Below Tier 3 |
| `STANDARD` | Limited risk | Tier 3 |
| `REGULATED` | Adjacent to high-risk (regulated use, not Annex III) | Tier 2 |
| `HIGH_RISK` | High-risk under Annex III | Tier 1 |

Set per tenant:

```yaml
regulus:
  adk:
    model-risk:
      tenant-tier: STANDARD
```

Each model in the registry has its own tier:

```java
ModelRegistry registry = ModelRegistry.of(Map.of(
    "gemini-2.5-flash", Tier.STANDARD,
    "gemini-2.5-pro",   Tier.REGULATED,
    "gpt-4o",           Tier.REGULATED
));
RegulusModelRiskPlugin.tier(Tier.STANDARD, registry);
```

A call to `gemini-2.5-pro` (REGULATED) from a tenant on STANDARD is **blocked
at the `BeforeModelCallback` boundary** with a structured policy event citing
SS1/23 §3 and AI Act Annex III where applicable.

The plugin also classifies ADK's `ContainerCodeExecutor` and
`VertexAiCodeExecutor` as `HIGH_RISK` by default — they can execute arbitrary
code on the model's behalf, which is a different kind of materiality but
similar risk shape.

## Who decides which tier

- For the **AI Act**: the deployer (you) plus the provider (Google /
  OpenAI / Anthropic) decide jointly. The provider declares the AI system's
  intended purposes; the deployer self-classifies the actual use against
  Annex III. Get it wrong and a market surveillance authority can reclassify
  you.
- For **SS1/23**: the firm's model risk function (often within the second
  line of defence) — informed by materiality, complexity, and reliance.

Regulus doesn't do this assessment for you. It enforces the decision once
made.

## Next

- [Audit trails](audit-trails.md)
- [Plugin reference → RegulusModelRiskPlugin](../plugins/model-risk.md)
