# What is GRC?

**GRC** stands for **Governance, Risk, and Compliance**. Three disciplines
that organisations historically ran separately and now run as a single
operating model:

- **Governance** — who's accountable, what the policy says.
- **Risk** — what could go wrong, how big, how likely.
- **Compliance** — which laws and regulations apply and whether we satisfy
  them.

In a Fortune-500-shaped enterprise, GRC is its own function with its own
software stack — a "GRC tool" — that runs policy management, risk registers,
control libraries, audit management, and evidence repositories all in one
place.

## The GRC tool landscape (2026)

Top platforms by adoption: **Riskonnect, OneTrust, MetricStream, ServiceNow
IRM, LogicGate**. Plus the security-GRC variants (Drata, Vanta, Secureframe)
and the enterprise-incumbent IBM OpenPages and RSA Archer. ServiceNow's "AI
Control Tower" launched in 2026 specifically for AI risk.

What these tools do:

- **Policy management.** Draft, approve, lifecycle, distribute policies.
- **Risk register.** Catalogue risks with owner, severity, likelihood,
  treatment plan.
- **Control library.** The set of controls the firm has decided to
  implement, mapped to frameworks (NIST AI RMF, ISO 42001, etc.).
- **Control testing.** Periodic verification that each control is
  operating — usually evidence-based.
- **Audit management.** Plan, scope, run, report internal and external
  audits.
- **Evidence repository.** Where the control-testing records live.

What these tools do *not* do:

- **Enforce a control at runtime.** They don't sit in the AI agent's
  request path. They consume evidence *that the control fired*.
- **Generate the evidence.** Something has to produce the records they
  catalogue.

That's where Regulus comes in.

## Where Regulus fits

Regulus is the **runtime substrate** of a GRC program for AI agents:

- **Generates evidence per agent invocation** — the audit event stream is
  the raw material.
- **Maps evidence to controls** — every event carries the regulation
  citation (e.g. `Art. 25`) and the framework control id (e.g. NIST
  GAI-4, ISO 42001 A.7.3).
- **Pushes evidence to your GRC tool** — pluggable adapters for
  ServiceNow IRM, OneTrust AI Governance, MetricStream, generic webhook
  (LogicGate, Riskonnect, RSA Archer, IBM OpenPages, internal bespoke).

What you keep getting from your GRC tool:

- Policy authoring and approval workflows.
- The canonical risk register.
- Control mapping and gap analysis.
- Audit planning.

Regulus and the GRC tool are **complementary**, not competing. The pitch
to a buyer with an existing GRC stack: "you already have a GRC tool; the
gap is that it doesn't enforce or evidence anything at runtime — that's
what Regulus is for."

## Three Lines of Defence

GRC operates inside a **three-lines-of-defence** model:

| Line | Who | What |
|---|---|---|
| 1L | Engineering, business unit | Owns risk at source; runs controls |
| 2L | Risk, compliance, model risk | Independent oversight; policy enforcement |
| 3L | Internal audit | Independent assurance |

Each line consumes the same Regulus evidence stream differently — see
[Three Lines of Defence](../governance/three-lines/index.md).

## In one sentence

Regulus produces the structured, control-mapped, framework-aware evidence
your GRC tool needs to do its job; your GRC tool produces the policy and
risk view Regulus enforces against.

## Where to read next

- [Three Lines of Defence](../governance/three-lines/index.md)
- [GRC integration overview](../governance/grc/index.md)
- [Evidence schema](../governance/evidence-schema.md)
