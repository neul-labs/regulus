# ADR-005: EU AI Act control mapping

- Status: Accepted
- Date: 2026-03-22
- Supersedes: —

## Context

The EU AI Act (Regulation EU 2024/1689) came into force in phased deadlines
from 2025 onwards. As an ADK-extension framework targeting EU agent
deployers, Regulus must enable users to satisfy AI Act obligations through
the same control surface they use for everything else (GDPR, FCA, etc.).
We need to decide how AI Act articles map onto our control mechanisms,
where the mapping is explicit, and where we deliberately do not cover an
article.

## Decision

Ship an `EuAiActProfile` whose `controls()` set binds Regulus mechanisms
to specific articles, as follows:

- `model-risk-tier` → Art. 9 (risk management).
- `audit-trail` → Art. 12 (logging).
- `transparency-disclosure` → Art. 13 (transparency).
- `dual-control-kill-switch` → Art. 14 (human oversight).
- `accuracy-robustness-cybersecurity` → Art. 15 (informational binding —
  Regulus is part of the cybersecurity story, not the accuracy story).
- `post-market-monitoring` → Art. 16.
- `deployer-obligations` → Art. 26.
- `annex-iii-classification` → Annex III (informational binding — the
  classification is the deployer's decision; Regulus stores and audits
  it).

Retention defaults to 180 days raw + 5 years summary, exceeding the Act's
minimum 6 months.

## Out of scope (deliberately)

- Art. 10 (data governance for training data) — we operate on inference,
  not training.
- Arts. 43–47 (conformity assessment) — a paperwork / certification
  process for providers, not deployers.
- Art. 27 (fundamental rights impact assessment) — deployer process; we
  produce evidence inputs.
- Chapter V (GPAI model provider obligations) — applies to the foundation-
  model provider (Google/Anthropic/etc.), not the deployer.

## Consequences

Positive: explicit, citable mapping; auditor-friendly evidence pack.

Negative: the mapping must be reviewed each time the AI Office issues
guidance amending Annex III or interpreting an article. We commit to
tracking this through a regular regulation-review cadence.
