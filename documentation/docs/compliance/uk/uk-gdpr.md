# UK GDPR + DPA 2018

## In one sentence

The UK's data-protection law: GDPR's substance copied into UK statute post-
Brexit, enforced by the ICO, with the Data Protection Act 2018 layering in
UK-specific provisions.

## Who does it apply to?

- Anyone processing personal data in the UK.
- Anyone established in the UK who processes personal data anywhere.
- Non-UK firms offering goods or services to UK residents, or monitoring
  their behaviour.

If your agent serves UK customers, you're in scope — even if you also serve
EU customers (where GDPR applies in parallel). Most firms end up complying
with both via the same code path.

## The two-minute explainer

When the UK left the EU in 2020, it copied the GDPR's text into the UK
statute book — UK GDPR — and amended it slightly to remove EU-specific
machinery. Day-to-day for engineers, **UK GDPR and EU GDPR look almost
identical.** The differences are mostly:

- The regulator is the **Information Commissioner's Office (ICO)**, not the
  EDPB / national DPAs.
- International transfer paperwork uses the **IDTA** or the **UK Addendum**
  to EU SCCs, not EU SCCs directly.
- Article numbering is mirrored, citations write as "UK GDPR Art. X" to
  disambiguate.
- The **Data Protection Act 2018** layers in national specifics: law-
  enforcement processing, intelligence services, exemptions, DPO appointment
  thresholds.

The EU adequacy decision (UK ↔ EU transfer free flow) renews periodically
and is politically fragile; treat it as conditional. Most engineers run a
single GDPR-shaped pipeline and configure the transfer machinery via SCCs /
IDTA / UK Addendum based on the destination.

## What it actually requires of an engineer

Identical engineering shape to EU GDPR:

- Lawful basis recorded per processing activity.
- Storage-limitation retention.
- Privacy by design (redaction, minimisation).
- Records of processing (Art. 30).
- Subject rights (Arts. 15–22).
- 72-hour ICO breach notification (Art. 33).
- International-transfer paperwork (Arts. 44–49).

UK-only flourishes:

- **ICO incident notification** has a specific portal and intake schema.
  Audit pipeline should be able to produce an ICO-shaped report.
- **Special category data** under DPA 2018 includes some categories beyond
  GDPR Art. 9 (e.g. immigration status processed in limited contexts).
- **Children's data** processing thresholds are slightly different from EU
  GDPR (UK ICO Age-Appropriate Design Code).

## What Regulus does for you

Same plugin surface as the GDPR profile; UK-specific details:

- `RegulusAuditPlugin` includes UK GDPR-shaped citations on policy events.
- Residency defaults to `europe-west2` (UK only) for the UK GDPR profile.
- Audit pipeline can emit ICO-incident-shaped events when severity tagging
  triggers (configured per tenant).

## Saves you ~

Same baseline as GDPR (~12 engineer-weeks for the foundation) plus ~1 week
to wire ICO-specific incident shape if you didn't have it already.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [uk-gdpr]
```

## Code: production

For a UK firm also serving EU customers, run both:

```yaml
regulus:
  compliance:
    profiles: [uk-gdpr, gdpr]
  adk:
    residency:
      allowed-regions: [europe-west2]   # UK-only; widen to EU regions if also serving EU directly
```

The composite picks the intersection of allowed regions (here `europe-west2`,
because UK GDPR profile lists only it). If you genuinely serve EU customers
on EU infrastructure, set the YAML allowlist to broaden — the composite
picks the YAML value when present.

## How to verify

Identical to GDPR (see [GDPR → How to verify](../eu/gdpr.md#how-to-verify)),
plus:

- ICO-incident export: trigger a synthetic high-severity event; verify the
  audit pipeline produces an ICO-shaped notification record within 72 hours.

## What an auditor will ask

The ICO's questions overlap heavily with the EDPB's — see the GDPR page —
plus:

1. **"How does the UK Addendum apply to your US transfers?"** Operational
   answer; show the data flows in/out of the UK.
2. **"How is your retention different for UK FCA / PRA records?"** If the
   `fca-sysc` or `pra-*` profiles are also active, retention is 5+ years
   regardless of UK GDPR's storage-limitation pressure — explain the
   regulator-side override.

## What this doesn't cover

- **Choosing your lawful basis.** Same as GDPR.
- **DPIA execution.** Same as GDPR.
- **IDTA / UK Addendum execution.** Paperwork.
- **PECR (cookies, e-marketing).** Separate UK statute; not in scope.

## Citations

- UK GDPR — https://www.legislation.gov.uk/eur/2016/679/contents
- Data Protection Act 2018 — https://www.legislation.gov.uk/ukpga/2018/12/contents
- ICO guidance — https://ico.org.uk/for-organisations/
- Age-Appropriate Design Code — ICO 2020.
- International Data Transfer Agreement — ICO.
