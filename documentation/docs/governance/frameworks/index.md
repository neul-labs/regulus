# Governance frameworks

Voluntary frameworks Regulus maps to. Activation:

```yaml
regulus:
  governance:
    frameworks: [nist-ai-rmf, nist-ai-rmf-600-1, iso-42001]
```

The composite of active frameworks produces a single set of `FrameworkBinding`s
the evidence pipeline writes into every audit event.

## Shipped frameworks

| Framework id | Name | Kind | Notes |
|---|---|---|---|
| [`nist-ai-rmf`](nist-ai-rmf.md) | NIST AI Risk Management Framework 1.0 | Voluntary | US reference; GOVERN/MAP/MEASURE/MANAGE |
| [`nist-ai-rmf-600-1`](nist-ai-rmf.md#genai-profile) | NIST AI 600-1 Generative AI Profile | Voluntary | 12 GAI-specific risks (Jul 2024) |
| [`nist-ai-rmf-agent-interop`](nist-ai-rmf.md#agent-interop-profile-planned-q4-2026) | NIST AI RMF Agent Interoperability Profile | Voluntary | Planned Q4 2026; placeholder IDs |
| [`iso-42001`](iso-42001.md) | ISO/IEC 42001 — AI Management System | Certifiable | Same shape as ISO 27001; SoA mandatory for cert |
| [`iso-23894`](iso-23894.md) | ISO/IEC 23894 — AI risk management | Standard | Companion to 42001 |
| [`iso-23053`](iso-23053.md) | ISO/IEC 23053 — AI/ML framework | Standard | Reference architecture for AI/ML |

## Which frameworks to pick

Common combinations:

| Your context | Recommended set |
|---|---|
| US-anchored AI program | `nist-ai-rmf, nist-ai-rmf-600-1` |
| Selling into regulated buyers (cert-driven) | `iso-42001, iso-23894` |
| Both regions, mature program | `nist-ai-rmf, nist-ai-rmf-600-1, iso-42001` |
| Anticipating the Q4 2026 NIST agent profile | + `nist-ai-rmf-agent-interop` |

When in doubt: pick more. Frameworks compose multiplicatively; no
contradictions can arise because each framework's control ids are
scoped to the framework. Evidence emission produces one envelope per
matching binding, so the same audit event can satisfy multiple
framework citations at once.

## What's *not* a framework in Regulus

EU AI Act, GDPR, FCA SYSC, NHS DSPT, EHDS, etc. live in
[Compliance](../../compliance/index.md). They're regulations.

The line: cited via a numbered standard or function → framework. Cited
via a statute or regulation → regulation. See
[Concepts → Frameworks vs regulations](../../concepts/frameworks-vs-regulations.md).
