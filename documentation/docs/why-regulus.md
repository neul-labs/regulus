# Why Regulus

**Where Google ADK ends, regulated builds begin.**

ADK ships AI agents. Regulus ships AI agents your regulator accepts.

## The 30-second version

If you've used Google ADK, you've experienced the speed: an `App`, a couple
of `LlmAgent`s, a working conversational system in an afternoon. Wonderful
runtime. **But ADK's mission stops at the runtime.** It does not — and
should not — own:

- The audit trail your auditor will demand.
- The retention schedule your DPO will sign off on.
- The kill switch your operations runbook will exercise.
- The model-risk tier your second line of defence will assess.
- The framework-mapped evidence your GRC tool will catalogue.

Building those — properly — is *months* of work in a regulated firm. Each
one has nuanced regulatory citations, validation requirements, evidence
expectations. Doing it badly is worse than not doing it at all, because a
bad audit trail is a discoverable artefact in an enforcement action.

Regulus is the **runtime layer** that fills exactly that gap.

## What Google ADK gives you

A capable, opinionated agent runtime:

- **`App` container** with a `BasePlugin` SPI — the canonical extension
  point for cross-cutting behaviour.
- **`LlmAgent`** with `Before*` / `After*` callbacks at every interception
  point (agent, model, tool).
- **`ToolConfirmation`** — official human-in-the-loop primitive.
- **`SessionService`** / **`MemoryService`** / **`ArtifactService`** —
  managed implementations for Vertex AI, Firestore, Cloud Storage.
- **`EventCompactor`** with `BaseEventSummarizer` for context management.
- **A2A protocol** — `RemoteA2AAgent` + `AgentExecutor` for cross-agent
  communication.
- **Code executors** — `ContainerCodeExecutor`, `VertexAiCodeExecutor`.
- **`adk deploy`** to **Vertex AI Agent Engine**.

That's a substantial runtime. It is also explicitly *not a compliance
framework*.

## What Google ADK leaves to you

The regulator-facing surface. A typical bank, three months into "we'll
just build it ourselves," ends up with a backlog like this:

- **Enterprise identity plane** — one canonical `Principal` + `Claims`
  shape, adapters for OIDC / SAML / mTLS / service-account JWTs, tenant
  + jurisdiction + purpose + lawful-basis on every audit event, token
  expiry enforcement, trust-boundary documentation an auditor will
  accept. Without this, every call's `actor` field is an opaque string
  whose provenance you can't defend.
- **Audit pipeline** — structured event schema, immutability guarantees,
  retention windows per active regulation, summarisation past horizon,
  GDPR Art. 17 erasure path where allowed, signed events for SS1/23,
  tamper-evident hash chain that auditors verify offline.
- **PII redaction** — pattern library for NINO / IBAN / BIC / sort code /
  email / NHS Number, applied before the prompt leaves the JVM,
  re-applied on streamed output, with stable token IDs so the model can
  refer to entities without seeing them.
- **Policy guards** — purpose binding, consent enforcement, GDPR Art. 22
  safeguards, Consumer Duty outcome tagging, vulnerable-customer routing,
  tool allowlist.
- **Kill switch** — single-control activation, dual-control deactivation,
  monotonic audit, scoped per tenant + per model.
- **Model-risk tier** — registry of approved models with tiers, per-tenant
  ceilings, gating of `ContainerCodeExecutor` and `VertexAiCodeExecutor`.
- **Data residency** — region allowlist, fail-closed startup check, per-
  call validation, evidence export for auditors.
- **Compliant `SessionService` / `ArtifactService`** — residency at
  construction, CMEK enforcement, erasure path.
- **A2A envelope** — propagate the same policy / privacy / audit / kill
  switch checks across agent hops, with RFC 9421 HTTP Message Signatures
  for cross-org audit linking.
- **Framework mappings** — NIST AI RMF subcategory citations on every
  control, ISO 42001 Annex A binding, Statement of Applicability for
  certification.
- **GRC integration** — push evidence into ServiceNow IRM / OneTrust /
  MetricStream / your webhook receiver.

This is — minimally — a quarter of engineering time, and it has to keep
working as regulators issue new guidance, as new patterns emerge, as ADK
itself evolves. The "regulator's hand off the keyboard" problem is the
real cost.

## What Regulus does

The exact list above — as **`BasePlugin` implementations on ADK's official
seams**. Not Spring AOP. Not bytecode rewriting. Not a parallel runtime.
Six plugins, six services, an A2A envelope module, a governance framework
module, a GRC adapter module, a CLI to scaffold the lot in 30 seconds.

The same Regulus mechanism (e.g. `pii-redaction`) satisfies multiple
regulations *and* multiple frameworks simultaneously. Audit events carry
both `regulation_clause` (for the regulator) and `framework_control_id`
(for the framework). One substrate, multiple audiences — engineer, 2L,
3L, external assessor.

See [Plugins overview](plugins/index.md), [Services overview](services/index.md),
[Governance overview](governance/index.md).

## What Regulus is *not*

- A **policy management UI**. Drafting / approving / lifecycling policies
  lives in your GRC tool.
- A **risk register**. Same.
- An **audit workflow tool**. Same.
- A **legal opinion**. Your DPO / CAIO / SMF holder / legal team still
  earn their fees.
- A **substitute for certification work**. ISO 42001 audit happens
  externally; Regulus produces the SoA evidence pack.

Clear scope. Clear edges.

## Why this is "where ADK ends"

Because every item in the "What Google ADK leaves to you" list is
*outside* ADK's stated mission (Google said so on the ADK announcement)
and *inside* the regulator's. Google built the runtime; the regulator
expects the controls; Regulus bridges.

## Why this is "where regulated builds begin"

Because once the controls are in place, the **same substrate** serves
every audience:

| Line | Audience | What Regulus gives them |
|---|---|---|
| 1L | Engineers | Inline guardrails on the ADK `App` |
| 2L | Risk + compliance | Continuous evidence stream to the GRC tool |
| 3L | Internal audit | Tamper-evident audit trail with framework cites |
| 4L | Regulators / external assessors | A reproducible coverage matrix + SoA |

One emission, four audiences. That's the engineering win: regulated
builds *begin* once Regulus is in place, rather than ending in the audit
firefight.

## The 60s / 5min / 15min ladder

You can be live with all of this in 15 minutes:

1. **60s.** Run `regulus init` (see [Install the CLI](getting-started/install-cli.md)).
   The scaffold drops 12 files in a directory. Working repo.
2. **5min.** `cd <agent-name> && gradle wrapper && ./gradlew bootRun`.
   The agent boots. Regulus plugins register. Audit events flow.
3. **15min.** Send a test request. Inspect the audit event. Compare it to
   the one your hand-rolled agent would produce (none).

## Where to read next

- [Show me — the diff](show-me.md) — side-by-side code + audit event sample.
- [Install the CLI](getting-started/install-cli.md) — get the scaffold tool.
- [Security model](concepts/security-model.md) — if you're a security architect or enterprise IT lead asking "how does SSO plug into this?"
- [Security architecture](advanced/security-architecture.md) — threat model, identity contract, A2A signing, audit integrity.
- [Governance overview](governance/index.md) — if you're a governance leader.
- [Concepts → What is regtech?](concepts/regtech-intro.md) — if all this is new vocabulary.
