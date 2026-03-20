# ADR-009: Regtech-as-product-docs editorial standard

- Status: Accepted
- Date: 2026-04-14

## Context

Regulus' audience is software engineers and architects, the overwhelming
majority of whom have never touched financial regulation, GDPR articles,
the EU AI Act, NHS DSPT, or any of the rest of it. They know Java +
Spring + ADK; they don't know what SYSC means or what Annex III is.

Standard product documentation assumes domain familiarity and explains
the *product*. Regtech documentation often assumes legal familiarity and
explains the *regulation*. Neither serves this audience.

## Decision

Adopt a single editorial standard for every Regulus compliance page and
every plugin page: the **12-section regtech-explainer template**.

1. In one sentence — what this regulation / control is.
2. Who does it apply to? — concrete self-identification examples.
3. The two-minute explainer — narrative.
4. What it actually requires of an engineer — engineering language.
5. What Regulus does for you.
6. Saves you ~ — honest engineer-week estimate.
7. Code: minimal.
8. Code: production.
9. How to verify.
10. What an auditor will ask — 3-5 questions and where to point them.
11. What this doesn't cover — explicit out-of-scope.
12. Citations — articles, paragraphs, URLs.

Two further commitments:

- Every Concepts page is ≤ 800 words, plain-English, code-where-helpful.
  Every acronym in any compliance / plugin page hyperlinks to the
  Glossary or to its own Concepts page.
- A PR that adds a control without its docs page (or with a docs page
  that breaks the template) is sent back. The CONTRIBUTING checklist
  enforces this.

## Why this matters more than usual

The docs are the *product surface*. A regulator-bound architect who
reads three Regulus pages and walks away convinced they can deliver a
compliant agent is the entire conversion funnel. A page that requires a
DPO to translate it loses that architect.

## Out of scope

- Translation. English-only for now; if internationalisation happens,
  the same template applies.
- Video / interactive content. Markdown only.

## Consequences

Positive: predictable shape; auditor-friendly; reduces the legal-
familiarity barrier.

Negative: more upfront writing cost. Mitigated by the template being
predictable enough to reuse.
