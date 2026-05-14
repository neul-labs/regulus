# What is AI governance?

**AI governance** is the discipline of running an AI program in a way that
mature stakeholders — your board, your regulator, your auditor, your
customers — find defensible. It is broader than compliance.

- **Compliance** asks: are we satisfying the laws that apply to us?
- **AI governance** asks: are we doing the right things, in the right order,
  with the right people, with evidence, even where the law doesn't (yet)
  force us?

Most regulated firms run both. Compliance is the floor; governance is the
ceiling.

## The pillars

A working AI governance program touches at least these:

- **Accountability.** Named owners for each AI system, with a clear path to
  a Senior Management Function (SMF) holder, a Chief AI Officer (CAIO), or
  an equivalent.
- **Policy.** Documented rules for what's acceptable (purposes, data, model
  classes, deployment contexts) and what isn't (banned use cases,
  unacceptable risks).
- **Risk management.** A process for identifying, analysing, treating, and
  monitoring AI-specific risks (data quality, bias, confabulation, security,
  third-party).
- **Lifecycle controls.** Decision gates at design, validation, deployment,
  change, and decommissioning.
- **Transparency.** Information for users, deployers, customers, regulators.
- **Human oversight.** The ability for a person to interrupt, override, or
  refuse an AI's output.
- **Monitoring + incident management.** Continuous observation of operating
  AI systems and a documented response when something goes wrong.
- **Independent assurance.** Internal audit or external assessor evaluates
  the program against a framework.

## The shape of a mature program

A program with all of the above usually sits inside a **three-lines-of-
defence** operating model (which has its own page in this section):

- **1L** — Engineering and business unit owners.
- **2L** — Risk, compliance, model risk, privacy, security functions.
- **3L** — Internal audit, providing independent assurance.

And it anchors to a **voluntary framework** (NIST AI RMF, ISO/IEC 42001) so
the program can be benchmarked externally — and for vendors, certified.

## Where Regulus sits

Regulus is the **runtime control execution layer** of an AI governance
program. It does *not* replace the program, the GRC tool, or the policy
function. It:

- Enforces controls at runtime (policy, privacy, residency, kill switch,
  model-risk tier).
- Produces structured evidence per invocation.
- Feeds that evidence into your GRC tool via adapters.
- Maps each control to NIST AI RMF and ISO/IEC 42001 control identifiers so
  the program's framework story stays joined-up.

What Regulus is *not*:

- A policy management UI (drafting / approving / lifecycling policies).
- A risk register tool.
- An audit management workflow tool.
- A substitute for your DPO, CAIO, model-risk function, or internal audit.

Those live in your GRC tool (ServiceNow IRM, OneTrust, MetricStream, etc.).
Regulus feeds them.

## How this maps to the Regulus codebase

- `com.regulus.platform.compliance.ComplianceProfile` — what regulations
  require (mandatory).
- `com.regulus.platform.governance.GovernanceFramework` — what frameworks
  recommend (voluntary).
- The same Regulus mechanism (e.g. `pii-redaction`) appears in both —
  bound to GDPR Art. 25 *and* to NIST AI RMF GAI-4 *and* to ISO 42001
  A.7.x.
- The coverage matrix shows you all three views side by side.

## Where to read next

- [What is GRC?](grc-intro.md)
- [Frameworks vs regulations](frameworks-vs-regulations.md)
- [Governance section](../governance/index.md)
- [NIST AI RMF](../governance/frameworks/nist-ai-rmf.md)
- [ISO/IEC 42001](../governance/frameworks/iso-42001.md)
