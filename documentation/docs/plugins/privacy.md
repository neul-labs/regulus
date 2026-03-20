# RegulusPrivacyPlugin

## In one sentence

Detects PII patterns in prompts and model output, replaces them with stable
tokens, and never lets the raw values land in the audit trail.

## Who does it apply to?

Any agent that might receive personal data — which is essentially every
customer-facing agent. The plugin runs even when no profile is active, so
it doubles as good engineering hygiene.

## The two-minute explainer

The plugin hooks two callbacks:

- **`BeforeModelCallback` (mutating)** runs first. It scans the prompt body
  for matches against the configured patterns, replaces each with a stable
  token like `<NINO_1>`, and stores the mapping in the per-invocation
  context. The mutated prompt is what reaches the LLM; the raw value never
  leaves the JVM.
- **`AfterModelCallback`** runs on the response. It applies the same
  pattern set to the model's output before any downstream sink sees it —
  catching the case where the model happens to emit a number that looks
  like a NINO even though we didn't put one in.

Built-in patterns: NINO, IBAN, BIC, UK sort code, UK bank account number,
UK postcode, email, NHS Number. Add custom patterns via the builder.

## What it actually requires of an engineer

- Pick the patterns you need at agent build time (`withPatterns(...)`).
- Add custom patterns if your data has sector-specific shapes
  (e.g. customer IDs in a known format).
- Decide what to do with the redaction map in your application — usually:
  nothing. The map exists for *forensic reconstruction* by an authorised
  operator with access to the audit context.

## What Regulus does for you

- Stable token IDs (`NINO_1`, `NINO_2` if multiple) so a model can refer to
  the entity without seeing it.
- Output re-redaction so streamed responses are scrubbed before any other
  plugin sees them.
- Audit-friendly metadata: every event includes a `redactions: [...]` array
  listing what was removed.

## Saves you ~

- ~3 engineer-weeks for the pattern set, tests, and audit hook. Plus
  ongoing pattern maintenance as banking formats and regional IDs change.

## Code: minimal

```java
RegulusPrivacyPlugin privacy = RegulusPrivacyPlugin
    .withPatterns(
        RegulusPrivacyPlugin.BuiltInPattern.NINO,
        RegulusPrivacyPlugin.BuiltInPattern.IBAN,
        RegulusPrivacyPlugin.BuiltInPattern.EMAIL
    ).build();
```

## Code: production

```java
RegulusPrivacyPlugin privacy = RegulusPrivacyPlugin
    .withPatterns(
        RegulusPrivacyPlugin.BuiltInPattern.NINO,
        RegulusPrivacyPlugin.BuiltInPattern.IBAN,
        RegulusPrivacyPlugin.BuiltInPattern.BIC,
        RegulusPrivacyPlugin.BuiltInPattern.SORT_CODE,
        RegulusPrivacyPlugin.BuiltInPattern.UK_ACCOUNT_NUMBER,
        RegulusPrivacyPlugin.BuiltInPattern.UK_POSTCODE,
        RegulusPrivacyPlugin.BuiltInPattern.EMAIL,
        RegulusPrivacyPlugin.BuiltInPattern.NHS_NUMBER)
    .pattern("CUSTOMER_ID", Pattern.compile("\\bCUST-\\d{8}\\b"))
    .build();
```

The Spring starter wires the standard set automatically; add custom
patterns via a `@Bean` that produces a `RegulusPrivacyPlugin` and rely on
`@ConditionalOnMissingBean` to win.

## How to verify

- Send a prompt containing `AB123456C` → audit shows
  `redactions: [NINO_1]`; the LLM gets the redacted form.
- Add a synthetic NINO in the *response* (mock the model) → audit shows
  the response redaction also fired.

## What an auditor will ask

1. **"What PII types does this agent see?"** Built-in pattern list + any
   custom additions.
2. **"How do you know it works?"** Test fixtures in
   `BuiltInPatternsTest`.
3. **"What if a new PII type appears?"** Custom-pattern path; release
   cadence.

## What this doesn't cover

- **Detection of PII in non-text fields** (images, audio). Out of scope.
- **De-identification beyond redaction** (e.g. k-anonymity for analytics).
  Different problem.
- **Tracking which model has been exposed to which token historically.**
  Per-invocation only.

## Citations

- See [GDPR](../compliance/eu/gdpr.md), [UK GDPR](../compliance/uk/uk-gdpr.md),
  [NHS DSPT](../compliance/uk/nhs-dspt.md) for the regulatory context.
