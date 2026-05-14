# Governance

Pages for the audience that owns the AI governance program: CISO, Chief AI
Officer, Chief Risk Officer, Head of Model Risk, 2nd Line of Defence, and
Internal Audit. The developer-first sections elsewhere stay the runtime
truth; this section is the **program** layer.

If you've not read [Concepts → What is AI governance?](../concepts/ai-governance-intro.md)
and [Concepts → What is GRC?](../concepts/grc-intro.md) yet, do so first.
Pages here lean on that vocabulary.

## What lives in this section

- **[Frameworks](frameworks/index.md)** — how Regulus maps to NIST AI RMF
  (1.0 + GenAI Profile + Agent Interop Profile), ISO/IEC 42001, ISO/IEC
  23894, ISO/IEC 23053.
- **[Three Lines of Defence](three-lines/index.md)** — how Regulus serves
  1L (engineering), 2L (risk + compliance), 3L (internal audit).
- **[GRC integration](grc/index.md)** — pluggable adapters that fan
  Regulus evidence into ServiceNow IRM, OneTrust AI Governance,
  MetricStream, or a generic webhook receiver.
- **[Evidence schema](evidence-schema.md)** — the canonical
  `GrcEvidenceEnvelope` that all adapters emit.
- **[Program operating model](program-operating-model.md)** — RACI for an
  AI governance program with Regulus underneath.

## What Regulus is in one diagram

```
                            ┌────────────────────────────────┐
                            │     Your AI governance         │
                            │     program (off-Regulus)      │
                            │   policy / risk / audit / etc. │
                            └────────────────────────────────┘
                                          ▲
                                          │ evidence flow
                                          │ (GrcEvidenceEnvelope)
                                          │
       ┌──────────────────┐       ┌───────┴────────┐       ┌──────────────────┐
       │  ServiceNow IRM  │◄──────┤ Regulus GRC    │──────►│  OneTrust /      │
       │                  │       │ adapters       │       │  MetricStream /  │
       └──────────────────┘       └───────┬────────┘       │  Webhook         │
                                          │                └──────────────────┘
                                          │
                                          ▼
                       ┌──────────────────────────────────────┐
                       │   Regulus plugins on ADK App         │
                       │   policy / privacy / audit / kill    │
                       │   switch / model risk / residency    │
                       └──────────────────────────────────────┘
                                          │
                                          ▼
                       ┌──────────────────────────────────────┐
                       │      Google ADK 1.x application      │
                       └──────────────────────────────────────┘
```

Two layers downstream of your program: Regulus enforces controls at
runtime; Regulus' adapters fan evidence back upstream into the GRC tool
your program already uses.

## Read order

1. [Concepts → What is AI governance?](../concepts/ai-governance-intro.md)
2. [Concepts → What is GRC?](../concepts/grc-intro.md)
3. [Frameworks → NIST AI RMF](frameworks/nist-ai-rmf.md)
4. [Three Lines of Defence overview](three-lines/index.md)
5. [GRC integration overview](grc/index.md)
6. [Program operating model](program-operating-model.md)
