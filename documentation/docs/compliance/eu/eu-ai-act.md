# EU AI Act

## In one sentence

The EU AI Act is the world's first horizontal AI law: it classifies AI
systems by risk, bans the worst, and imposes engineering obligations on the
rest, with the strictest applying to "high-risk" systems listed in Annex III.

## Who does it apply to?

If **any one** of these is true, you're in scope:

- You build an AI system that is **placed on the EU market** (sold or made
  available there).
- You **put an AI system into service** in the EU — including for your own
  internal use (this makes you a "deployer").
- You're outside the EU but the **output** of your AI system is used in the
  EU.
- You're a UK firm whose product reaches EU consumers, partners, or
  employees.

Practical test: if an EU resident can be on the receiving end of a decision
your agent influences, assume in-scope and read on.

## The two-minute explainer

Until 2024 there was no EU-wide AI law. National courts and the GDPR did the
work, awkwardly, because GDPR is about *personal data*, not AI as such. The
AI Act fills that gap. It came into force across 2025–2027 in phased
deadlines: prohibitions first, then transparency, then high-risk
obligations, then general-purpose AI rules.

The shape is a **risk pyramid**. The base — minimal-risk systems — has
almost no obligations. Above that, limited-risk systems (chatbots, deepfake
generators) owe basic transparency to users. Above that, **high-risk
systems** owe the bulk of the Act: risk management, data governance,
logging, transparency, human oversight, accuracy, post-market monitoring,
conformity assessment. At the top, **prohibited systems** are banned
outright — social scoring by governments, manipulative subliminal
techniques, real-time biometric ID in public spaces (with narrow law-
enforcement exceptions).

Two role distinctions matter: the **provider** (who builds and places the
system on the market) and the **deployer** (who puts it into service). Most
firms using ADK to build internal or customer-facing agents are deployers of
those agents; the LLM provider underneath is a provider of a "general-purpose
AI model" with its own (lighter) obligations.

High-risk classification flows from **Annex III**, which lists eight use-case
families: biometric ID, critical infrastructure, education, employment,
essential services (including credit scoring), law enforcement, migration
and border control, and administration of justice. If your agent operates
in any of these contexts, you're high-risk and the full obligation stack
applies.

## What it actually requires of an engineer

- **Logging (Art. 12).** Automatic, machine-readable logs sufficient to trace
  the system's functioning. Keep them at least 6 months; longer for high-risk
  systems in regulated sectors. *Regulus' audit trail is built for this; the
  opt-in [hash-chain integrity](../../advanced/security-architecture.md#audit-integrity)
  adds tamper-evidence.*
- **Human oversight (Art. 14).** A natural person must be able to interrupt,
  override, or refuse to follow the AI's output. For high-risk: documented
  oversight procedure and training. *Regulus' kill switch + ADK
  ToolConfirmation map directly.*
- **Accuracy, robustness, cybersecurity (Art. 15).** Quantitative measures
  and reporting. *Regulus is part of the cybersecurity story; see
  [Security architecture](../../advanced/security-architecture.md) for the
  threat model, identity contract, and A2A signing. Accuracy is outside
  our scope.*
- **Risk-management system (Art. 9).** Lifecycle process, documented, kept
  current. *Regulus contributes the technical evidence; the system itself
  is your governance function.*
- **Transparency to deployers / users (Arts. 13 + 26).** Provenance and
  instruction information. *Regulus emits provenance fields on every
  invocation.*
- **Post-market monitoring (Art. 16).** Detect and report drift, incidents,
  serious harm. *Regulus audit log feeds this.*
- **Risk-tier classification (Annex III).** *You* do this; Regulus stores
  the assessment per model.

## What Regulus does for you

- `RegulusAuditPlugin` emits structured events on every model and tool call,
  with the AI Act-required fields (model_id, model_version, ai_act_risk_tier,
  human_oversight_status).
- `RegulusRetentionEventCompactor` retains raw events for **180 days** under
  the `eu-ai-act` profile, summarises for 5 years thereafter (longer than the
  Act's 6-month minimum, because most deployers also have GDPR / FCA / DORA
  retention that overrides).
- `RegulusPolicyPlugin` enforces purpose binding and short-circuits to
  `RequireConfirmation` for high-risk model calls flagged with
  `automated_legal_effect`.
- `RegulusKillSwitchPlugin` is the Art. 14 "stop button," with dual-control
  on deactivation.
- `RegulusModelRiskPlugin.Tier.HIGH_RISK` is the per-tenant ceiling; tier
  registry holds the classification per model.

## Saves you ~

A senior backend engineer with no prior regtech, no existing tooling, will
need roughly:

- ~2 weeks to build the audit-event schema and a Kafka pipeline that retains
  appropriately.
- ~1 week to define a risk-tier registry that holds per-model classification.
- ~1.5 weeks to implement a working kill switch with operator UI and dual-
  control deactivation.
- ~0.5 week to wire human-oversight intercepts at the right callbacks.

Total: ~5 engineer-weeks, plus ongoing tracking of Annex III amendments and
sector-specific guidance from the AI Office. Regulus is one dependency.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [eu-ai-act]
```

This activates the audit, retention, kill-switch, and policy plugins with
AI Act defaults. Other plugins remain available but unconfigured.

## Code: production

```yaml
regulus:
  compliance:
    profiles: [eu-ai-act, gdpr]   # AI Act on its own is rarely enough
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.ai-act.v1
    kill-switch:
      enabled: true
      dual-control: true
    model-risk:
      tenant-tier: REGULATED       # or HIGH_RISK if Annex III in scope
    residency:
      allowed-regions: [europe-west1, europe-west2, europe-west3]
```

In Java:

```java
ComplianceProfile profile = ComplianceProfiles.compose(
    List.of("eu-ai-act", "gdpr"));

App app = App.builder("annex-iii-agent", rootAgent)
    .plugin(RegulusPolicyPlugin.fromProfile(profile))
    .plugin(RegulusAuditPlugin.forProfile(profile).toKafka("audit.ai-act.v1").build())
    .plugin(RegulusKillSwitchPlugin.dualControl())
    .plugin(RegulusModelRiskPlugin.tier(Tier.HIGH_RISK))
    .plugin(RegulusDataResidencyPlugin.fromPolicy(profile.residency()))
    .build();
```

## How to verify

- `./gradlew regulusComplianceMatrix` — every AI Act article we cover lands
  in the matrix.
- Send a request with `automated_legal_effect=true` → audit shows the policy
  short-circuit with citation `Art. 22` (GDPR) and `Art. 14` (AI Act).
- Trip the kill switch → next request gets `KillSwitchActive` with operator
  attribution and timestamp.
- Inspect the Kafka audit topic: every event has `model_id`, `model_version`,
  `ai_act_risk_tier`, `human_oversight_status`.

## What an auditor will ask

1. **"Show me a sample of your AI system logs."** Point to the audit topic
   and one full request trace.
2. **"How did you classify this system under Annex III?"** Show
   `RegulusModelRiskPlugin` configuration plus your written assessment.
3. **"Demonstrate human oversight."** Show the kill-switch deactivation flow
   (dual control, recorded operators, audit trail).
4. **"What's your retention?"** Point to `eu-ai-act` profile retention (180
   days raw, 5 years summary) and the compactor task.
5. **"How do you find a specific event from six months ago?"** Show subject-
   linked query (the audit schema's `subject_id` field).

## What this doesn't cover

- **Conformity assessment** (Arts. 43–47). Required for high-risk systems
  before placing on the market. This is a paperwork / certification process,
  not a code feature. We give you the evidence; you commission the assessment.
- **Fundamental rights impact assessment** (Art. 27). Deployer-side process.
  Document outside Regulus.
- **Accuracy and robustness metrics** (Art. 15). You measure model
  performance; we audit it.
- **Generative AI / GPAI model obligations** (Chapter V). Those fall on
  Google/Anthropic/etc. as providers of the foundation model — not on you as
  deployer.
- **Whether your specific agent is high-risk under Annex III.** That's your
  call (with legal advice). We enforce the consequences once decided.

## Framework mapping

The EU AI Act sits naturally alongside two voluntary frameworks Regulus
also supports:

- **NIST AI RMF** — AI Act Art. 9 (risk mgmt) ↔ NIST `MAP-4.1`. Art. 12
  (logging) ↔ `GOVERN-1.5` + `MEASURE-1.1`. Art. 14 (human oversight) ↔
  `GOVERN-4.1`. Art. 16 (post-market monitoring) ↔ `MANAGE-4.1`. Art. 26
  (deployer obligations) ↔ `GOVERN-2.1`.
- **NIST AI 600-1 GenAI Profile** — relevant for generative-AI deployers
  in particular. GAI-2 (confabulation) and GAI-7 (human-AI configuration)
  most directly overlap.
- **ISO/IEC 42001** — AI Act Art. 12 (logging) ↔ ISO `A.6.2.7`. Art. 14
  (oversight) ↔ `A.6.2.8`. Art. 26 (deployer obligations) ↔ `A.9.2`.

See [Governance → frameworks](../../governance/frameworks/index.md) for
the full bindings, and the [Coverage matrix](../coverage-matrix.md) for
the cross-reference view.

## Citations

- Regulation (EU) 2024/1689 — full text: https://eur-lex.europa.eu/eli/reg/2024/1689/oj
- Art. 9 — risk-management system.
- Art. 12 — record-keeping (logging).
- Art. 13 — transparency and provision of information to deployers.
- Art. 14 — human oversight.
- Art. 15 — accuracy, robustness and cybersecurity.
- Art. 16 — provider obligations.
- Art. 26 — deployer obligations.
- Art. 27 — fundamental rights impact assessment.
- Annex III — high-risk AI systems list.
- AI Office — https://digital-strategy.ec.europa.eu/en/policies/ai-office
