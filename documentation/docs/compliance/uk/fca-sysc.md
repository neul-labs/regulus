# FCA SYSC + Consumer Duty

## In one sentence

The FCA's "how you must run your firm" rulebook (SYSC) plus the outcomes-
based duty to act in retail customers' interests (Consumer Duty FG22/5,
PS22/9) — together they shape what a regulated UK firm has to do around any
customer-facing AI agent.

## Who does it apply to?

- UK firms authorised by the FCA (banks, insurers, brokers, wealth managers,
  payment institutions, e-money institutions, consumer credit firms, etc.).
- Some specific obligations also apply to firms regulated only by the PRA
  via the "approved persons" / "Senior Managers and Certification Regime."

If your agent helps a retail customer with a financial decision and your firm
is FCA-authorised, you are in scope. Wholesale-only firms have a lighter
Consumer Duty footprint but still owe SYSC.

## The two-minute explainer

Two layers.

**SYSC** is a section of the FCA Handbook covering Senior Management
Arrangements, Systems and Controls. It is *not* AI-specific; it predates AI
agents by 25 years. Its core demands are: senior people are accountable
(SYSC 4), systems and controls are proportionate to the business (SYSC 6,
SYSC 13), records are kept (SYSC 9), outsourcing is governed (SYSC 13.9).
When the FCA looks at an AI deployment, it applies SYSC to it: who's
accountable, what controls exist, what records are kept, how the outsource
to the LLM provider is governed.

**Consumer Duty** (FG22/5 and PS22/9, in force from 2023) is newer and
outcome-focused. Firms must deliver good outcomes for retail customers
across four cross-cutting outcomes: **products and services**, **price and
value**, **consumer understanding**, **consumer support**. There's a separate
duty for **vulnerable customers**. The Duty replaced a checklist mindset with
an evidence-of-outcomes mindset — you have to be able to show *outcomes*,
not just procedural compliance.

For an AI agent, the question becomes: does this agent help or hurt the four
outcomes? A retention-bot that confuses customers or upsells inappropriately
fails Consumer Duty even if it ticks SYSC boxes.

## What it actually requires of an engineer

- **Senior Management attribution (SYSC 4 + SMCR).** Every consequential
  action should be traceable to an SMF holder ("This action falls under
  SMF24's responsibility"). The audit needs SMF in the event.
- **Outsourcing governance (SYSC 13.9).** The LLM provider is an outsource.
  Records of why it was chosen, what alternatives exist, exit plan, audit
  rights. *Operational paperwork; Regulus surfaces the technical state.*
- **Record-keeping (SYSC 9).** Most records 5 years; MiFID-relevant records
  7 years. Audit retention must match.
- **Four-outcomes alignment (Consumer Duty).** Per-agent justification
  showing which of the four outcomes the agent supports and how. Audit needs
  the outcome on the event.
- **Vulnerable-customer protection.** Agents handling vulnerable customers
  need enhanced controls. Flag the customer in invocation context and route
  through dual-control / HITL.

## What Regulus does for you

- `RegulusAuditPlugin` emits `smf_holder`, `consumer_duty_outcome`,
  `vulnerable_customer_flag`, `fca_lei` on every event when the `fca-sysc`
  profile is active.
- `RegulusPolicyPlugin` produces `RequireConfirmation` when
  `vulnerable_customer=true` is in the invocation context, mapping to ADK
  `ToolConfirmation` (HITL).
- Retention is set to 5 years (raw) + 7 years (summary) — covers SYSC 9 and
  MiFID II.
- The `fca-sysc` profile pins `europe-west2` for UK-only firms by default;
  override via YAML for firms with EU establishments.
- The `RegulusModelRiskPlugin` materiality tier (REGULATED / HIGH_RISK)
  feeds into SYSC's proportionality test.

## Saves you ~

- SMF-aware audit pipeline: ~2 engineer-weeks.
- Consumer Duty outcome tagging + reporting view: ~3 engineer-weeks.
- Vulnerable-customer routing into dual-control: ~1.5 engineer-weeks.
- Outsourcing register integration (regulator-facing exports): ~2 engineer-
  weeks.

Total: ~8.5 engineer-weeks, plus tracking FCA Dear CEO letters and
Consumer Duty interpretations.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [fca-sysc]
```

## Code: production

```yaml
regulus:
  compliance:
    profiles: [fca-sysc, pra-ss1-23, uk-gdpr]
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.fca.v1
    kill-switch:
      enabled: true
      dual-control: true
    model-risk:
      tenant-tier: REGULATED
    residency:
      allowed-regions: [europe-west2]
      require-cmek: true
```

In Java, with explicit per-request tags:

```java
PolicyContext context = new PolicyContext(
    "retail-mortgage-advice",                   // purposeCode
    customerId,                                  // subjectId
    "user:" + sessionUser.getId(),               // actor
    "model",                                     // targetKind
    "gemini-2.5-pro",                            // targetId
    Map.of(
        "smf_holder", "SMF24:Jane Smith",
        "consumer_duty_outcome", "support",
        "vulnerable_customer", String.valueOf(isVulnerable),
        "fca_lei", "213800ABC123"
    ));
```

`RegulusPolicyPlugin` reads `vulnerable_customer=true` and short-circuits to
`RequireConfirmation` — the agent pauses for HITL approval before proceeding.

## How to verify

- Invoke with `vulnerable_customer=true` → audit shows
  `RequireConfirmation` with citation `FG22/5 §4`.
- Pull a sample event: `smf_holder`, `consumer_duty_outcome`, `fca_lei`
  present and populated.
- Trip the kill switch → next request `KillSwitchActive`; deactivation needs
  two SMF holders (configurable to require SMF identity on the operator).
- Retention check: audit topic configured for 5+ years; compaction task
  scheduled.

## What an auditor will ask

1. **"Who is responsible for this AI system?"** SMF holder per event;
   reference to the firm's SMCR records.
2. **"What does Consumer Duty look like in your evidence?"** Outcome tagging
   per event; outcome-grouped dashboard.
3. **"How do you protect vulnerable customers?"** Flagging path + HITL
   demo + audit attribution to the confirming operator.
4. **"Show me your outsourcing register entry for the LLM provider."** Off-
   Regulus but linked from the audit pipeline.
5. **"Demonstrate kill-switch operation."** Activation drill, dual-control
   deactivation drill, post-incident audit reconstruction.

## What this doesn't cover

- **SMCR identity management.** Your IDP / HR system holds SMF mappings;
  Regulus records them on events but doesn't manage them.
- **MiFID II transaction reporting.** Different regulator interface; not in
  scope.
- **Retail conduct rules** (COBS, ICOBS). Behavioural rules about how
  products are sold; we record the agent's action, you ensure the product
  flow satisfies COBS.
- **Operational resilience under SS1/21 + FG21/3.** Some overlap with our
  audit + kill switch, but the resilience programme as a whole is a separate
  workstream.

## Citations

- FCA Handbook SYSC — https://www.handbook.fca.org.uk/handbook/SYSC/
- SYSC 4 — Senior Management Arrangements.
- SYSC 9 — Record-keeping.
- SYSC 13 — Operational risk; outsourcing.
- FG22/5 — Final non-Handbook Guidance for firms on the Consumer Duty.
- PS22/9 — Policy Statement implementing the Consumer Duty.
- Senior Managers and Certification Regime — FCA Handbook SUP / SYSC.
