# Concepts

The pages in this section teach the vocabulary that every Compliance page
later relies on. They are written for software engineers and architects who
have never touched financial regulation, GDPR articles, the EU AI Act, NHS
DSPT, or any of the rest of it. No prior exposure assumed.

You don't need to read every page — but you should know **where each lives**,
because the Compliance section will hyperlink back here every time it uses a
term that isn't pure CS vocabulary.

> Already convinced? Skip to [Why Regulus](../why-regulus.md) for the
> narrative, or [Show me — the diff](../show-me.md) for the audit-event
> proof.

## Pages

**Foundations** (read these first):

1. **[What is regtech?](regtech-intro.md)** — the engineer's version of the
   field's definition and why it matters for AI agents.
2. **[What is AI governance?](ai-governance-intro.md)** — the broader
   discipline that compliance sits inside. The audience the Governance
   section is written for.
3. **[What is GRC?](grc-intro.md)** — three letters, three disciplines, and
   where Regulus fits relative to the GRC tooling landscape (ServiceNow IRM,
   OneTrust, MetricStream).
4. **[Frameworks vs regulations](frameworks-vs-regulations.md)** — why
   Regulus separates them in code and in docs.

**Regulator-facing vocabulary**:

5. **[EU vs UK landscape](eu-uk-landscape.md)** — one-page map of regulators
   and laws. Pin this open while you read anything else.
6. **[Controller, processor, deployer](controller-processor-deployer.md)** —
   the three roles a developer keeps tripping over.
7. **[Security model](security-model.md)** — the canonical `Principal` +
   `Claims` shape every IdP adapter mints; the trust boundaries; what
   Regulus refuses to do.
8. **[Risk tiers](risk-tiers.md)** — EU AI Act's pyramid, PRA SS1/23's model
   risk tiers, and how Regulus maps both.
9. **[Audit trails](audit-trails.md)** — what auditors actually look at, in
   engineering terms.
10. **[Data residency](data-residency.md)** — why the LLM endpoint's location
    is a legal question.
11. **[Dual control / 4-eyes](dual-control.md)** — the banking primitive that
    the EU AI Act and PRA both demand for AI.
12. **[Glossary](glossary.md)** — every acronym, one line each.
