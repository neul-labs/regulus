# EU vs UK landscape

The EU and the UK have *similar but different* regulatory regimes for AI and
data. Regulus ships paired profiles for the overlaps and separate profiles
where they diverge. If you serve customers in both jurisdictions — which most
production AI agents do — you almost certainly need both sides switched on.

This page is the one-page map. Pin it open while you read the Compliance
section.

## Regulators

| Regulator | Scope | Jurisdiction |
|---|---|---|
| **EDPB** — European Data Protection Board | Coordinates national DPAs on GDPR | EU |
| **National DPAs** (e.g. CNIL, BfDI, Garante) | Enforce GDPR in each member state | EU |
| **AI Office** | Enforces the EU AI Act | EU |
| **ENISA** | EU cybersecurity, including NIS2 | EU |
| **EIOPA / ESMA / EBA** | Sectoral financial supervision | EU |
| **ICO** — Information Commissioner's Office | UK GDPR, DPA 2018 | UK |
| **FCA** — Financial Conduct Authority | UK conduct of financial services | UK |
| **PRA** — Prudential Regulation Authority | UK prudential supervision of banks/insurers | UK |
| **MHRA** | UK medical devices (incl. AI as medical device) | UK |
| **NHS England** + **Data Guardian** | NHS DSPT, IG SIRI | UK |

## Laws that touch AI agents

Grouped by what they're about, with the Regulus profile id that covers each.

### Data protection (the spine)

- **EU GDPR** (`gdpr`) — Regulation EU 2016/679. The original.
- **UK GDPR + Data Protection Act 2018** (`uk-gdpr`) — same shape as EU GDPR
  since Brexit, ICO enforced. Practical engineering is nearly identical.

### AI-specific

- **EU AI Act** (`eu-ai-act`) — Regulation EU 2024/1689. Risk tiers, logging,
  human oversight, transparency, deployer obligations. Comes into force in
  phased dates 2025–2027.
- **UK AI Regulation Principles** — non-binding to date; the FCA, PRA, ICO,
  and MHRA each apply their existing rulebooks to AI. *No dedicated Regulus
  profile needed — covered by `fca-sysc`, `pra-ss1-23`, `uk-gdpr` etc.*

### Cyber + ICT resilience

- **NIS2** (`nis2`) — Directive EU 2022/2555. Cybersecurity for essential and
  important entities. 24-hour early warning, 72-hour notification.
- **DORA** (`dora`) — Regulation EU 2022/2554. ICT operational resilience for
  EU financial services.
- **UK Operational Resilience** — PRA / FCA SS1/21 + FG21/3. Less prescriptive
  than DORA; covered by `fca-sysc` + `pra-ss2-21`.

### Sector-specific (financial services)

- **EU MiFID II** — record-keeping requirements implicit in `gdpr` retention
  + `dora` ICT records.
- **FCA Handbook** (`fca-sysc`) — SYSC 4 (senior management), SYSC 9
  (record-keeping), SYSC 13 (outsourcing).
- **FCA Consumer Duty** (`fca-sysc`) — FG22/5, PS22/9. Four customer
  outcomes.
- **PRA SS1/23** (`pra-ss1-23`) — Model Risk Management Principles for UK
  banks.
- **PRA SS2/21** (`pra-ss2-21`) — Outsourcing and Third-Party Risk Management.

### Sector-specific (health)

- **NHS DSPT** (`nhs-dspt`) — Data Security and Protection Toolkit. Annual
  assessment for any organisation handling NHS data.
- **EHDS** (`ehds`) — Regulation EU 2025/327. European Health Data Space:
  primary use (patient access) and secondary use (research, policy).

## Where the EU and UK actually diverge

- **Cross-border transfers.** EU → UK is currently covered by an EU adequacy
  decision, but it expires periodically and is politically contested. UK → US
  is the IDTA / UK Addendum to SCCs; EU → US is the EU-US Data Privacy
  Framework (different shape, different enforceability).
- **AI-specific framework.** The EU has the AI Act; the UK has principles
  applied via existing sectoral regulators. Net effect for an engineer:
  similar controls, different paperwork.
- **Health data.** EHDS is EU-wide; the UK's NHS rules are operationally
  more prescriptive (smartcard identities, IG SIRI process) but legally
  sit on top of UK GDPR.
- **ICT resilience.** DORA is more prescriptive than the UK's operational
  resilience framework. If you serve EU financial services, DORA is the higher
  bar.

## Which profiles you probably need

| Your situation | Probable profile set |
|---|---|
| EU/UK consumer-facing AI agent | `eu-ai-act, gdpr, uk-gdpr` |
| UK retail bank | `eu-ai-act, uk-gdpr, fca-sysc, pra-ss1-23, pra-ss2-21` |
| EU investment manager | `eu-ai-act, gdpr, dora` |
| EU/UK essential entity (energy, transport, etc.) | `eu-ai-act, gdpr, uk-gdpr, nis2` |
| NHS / UK health | `eu-ai-act, uk-gdpr, nhs-dspt` |
| EU health-data secondary use | `eu-ai-act, gdpr, ehds` |

When in doubt: pick more profiles. Regulus' composite always takes the
*stricter* requirement when profiles disagree on a setting like retention or
residency, so over-selecting is safe (though not free — longer retention costs
storage).

## Next

- [Controller, processor, deployer](controller-processor-deployer.md)
- [Risk tiers](risk-tiers.md)
