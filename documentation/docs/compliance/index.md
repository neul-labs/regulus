# Compliance

One page per regulation Regulus ships a profile for. Each page follows the
same shape so a reader can self-identify in 20 seconds and find the
engineering answer in two minutes — see
[ADR-009](https://github.com/neul-labs/regulus/blob/main/docs/decisions/ADR-009-regtech-as-product-docs.md) for the
editorial standard.

1. In one sentence — what this regulation is.
2. Who does it apply to? — concrete self-identification examples.
3. The two-minute explainer — narrative.
4. What it actually requires of an engineer.
5. What Regulus does for you.
6. Saves you ~ — honest engineer-week estimate.
7. Code: minimal.
8. Code: production.
9. How to verify.
10. What an auditor will ask.
11. What this doesn't cover — explicit out-of-scope.
12. Citations.

If a regulation feels unfamiliar, jump back to [Concepts](../concepts/index.md)
first — every page here assumes you know the
[EU vs UK landscape](../concepts/eu-uk-landscape.md) and the
[controller / processor / deployer trio](../concepts/controller-processor-deployer.md).

## EU

- [EU AI Act](eu/eu-ai-act.md) — Risk tiers, logging, human oversight, transparency
- [GDPR](eu/gdpr.md) — Data-protection foundation
- [DORA](eu/dora.md) — ICT operational resilience for EU financial services
- [NIS2](eu/nis2.md) — Cybersecurity for essential / important entities
- [EHDS](eu/ehds.md) — European Health Data Space

## UK

- [UK GDPR + DPA 2018](uk/uk-gdpr.md) — UK data protection
- [FCA SYSC + Consumer Duty](uk/fca-sysc.md) — UK conduct of financial services
- [PRA SS1/23](uk/pra-ss1-23.md) — Model risk management
- [PRA SS2/21](uk/pra-ss2-21.md) — Outsourcing and third-party risk
- [NHS DSPT](uk/nhs-dspt.md) — NHS data security toolkit

## Cross-cutting

- [Coverage matrix](coverage-matrix.md) — regulation × control × ADK hook × test fixture (now includes NIST AI RMF + ISO 42001 framework columns)
- [Time saved](time-saved.md) — honest cost of building each control yourself
- [Audit walkthrough](audit-walkthrough.md) — what we showed the auditor

## Related: AI governance frameworks

Regulations are mandatory. Frameworks like NIST AI RMF and ISO/IEC 42001
are voluntary best-practice anchors most mature operators adopt
*alongside* their regulation profiles. See the
[Governance section](../governance/index.md) and
[Concepts → Frameworks vs regulations](../concepts/frameworks-vs-regulations.md).
