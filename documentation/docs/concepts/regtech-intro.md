# What is regtech?

**Regtech** is the slice of software whose job is to satisfy a regulator. Not
to delight a user, not to ship a feature — to make the answer to "are you
compliant with X?" a clean "yes, here is the evidence."

It grew up after the 2008 financial crisis, when banks were drowning in
post-crisis regulation (Basel III, MiFID II, Dodd-Frank, the FCA's Senior
Managers Regime) and discovered that "we'll just hire more compliance people"
didn't scale. Software started doing the carrying. By the mid-2010s "regtech"
was a recognisable category alongside fintech: KYC engines, transaction
monitoring, trade-reporting pipelines, market-abuse surveillance.

Then AI agents happened.

## Why agents made regtech hot again

An agent is software with three properties that regulators care about:

- **It takes actions on behalf of a person.** That's a "controller →
  processor → data subject" question, which is the spine of GDPR.
- **It makes decisions that affect customers.** That's automated-decision
  territory: GDPR Art. 22, FCA Consumer Duty, EU AI Act Annex III.
- **It's opaque.** The model is a black box; the prompt is dynamic; the tools
  it picks aren't always predictable. Every existing model-risk and
  outsourcing rulebook now has to apply to something that wasn't in scope
  when it was written.

So regulation that was originally written for spreadsheets and credit-scoring
models had to extend to AI agents. The EU AI Act (2024/1689) is the clearest
example: it explicitly classifies AI systems into risk tiers and demands
logging, human oversight, transparency, and accuracy guarantees. The PRA's
SS1/23 in the UK does the same for financial-services models. DORA layers ICT
risk on top. NIS2 layers cyber on top of that.

The result: an AI agent now sits inside a stack of overlapping rulebooks, and
every customer-facing team has to demonstrate that the stack is satisfied.

## Where Regulus sits

Regulus is a **regtech library for AI agents on Google ADK**. It implements
the controls that regulators want — purpose binding, PII handling, dual
control, audit retention, residency, model risk tiering — as `BasePlugin`
implementations that you add to your ADK `App`.

We don't:

- Tell you whether *you* are compliant. That's your DPO / SMF holder / legal
  team's call. We give them the evidence they need to make it.
- Replace ADK. Regulus is an extension on official ADK seams. If ADK changes
  its plugin API tomorrow, we update against the change — but you don't lose
  ADK semantics.
- Promise compliance for every regulator. We ship profiles for the EU and UK
  regulators most agent teams encounter (full list at
  [Compliance overview](../compliance/index.md)); custom profiles are
  documented at [Operations → Custom profiles](../operations/custom-profiles.md).

## The mental model

If you remember one thing: **regtech is the audit trail of the unsexy decision.**
Every time you'd otherwise hard-code a behaviour, regtech replaces it with a
recorded, attributable, justifiable decision. Regulus makes that recording
automatic and the justification machine-readable, so when an auditor walks
in, the answer is a query, not a fire drill.

## Next

- [EU vs UK landscape](eu-uk-landscape.md) — the regulators and laws you'll
  see referenced everywhere.
- [Risk tiers](risk-tiers.md) — the framework two of the biggest regulations
  rely on.
- [Audit trails](audit-trails.md) — what an auditor actually looks at.
