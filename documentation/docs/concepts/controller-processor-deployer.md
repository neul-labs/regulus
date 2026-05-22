# Controller, processor, deployer

Three terms a developer reading GDPR or the EU AI Act keeps hitting. They look
similar; they aren't. Getting them wrong is how DPAs find you.

## The three roles

**Controller** (GDPR term). The party that **decides why and how** personal
data is processed. They're on the hook for the legal basis, the privacy
notice, and the answer to "why are you holding this data?".

**Processor** (GDPR term). The party that **processes personal data on
behalf of a controller**. They follow the controller's instructions; they
don't decide what the data is for. Cloud LLM providers usually sit here.

**Deployer** (EU AI Act term — equivalent to GDPR's controller for AI
systems, but specific to AI). The party that **puts an AI system into
service** in their own context. They're on the hook for human oversight,
keeping logs, monitoring, and handling incidents.

## In your ADK setup

For a typical Regulus-on-ADK app you've just built:

| Role | Who |
|---|---|
| **Controller** | Usually your firm. You decide whose data is processed, for what, and how. |
| **Deployer** | Same as above — your firm. Your `App` is the AI system being put into service. |
| **Processor** | The LLM provider (Vertex AI for Gemini, Anthropic's API, etc.) — they process your data to generate responses, on your instructions, per the model card / DPA. |
| **Sub-processor** | Cloud providers further down the chain. E.g. if Anthropic hosts on AWS, AWS is the sub-processor. |

Multi-tenant SaaS adds a twist: your *customer* is often the controller and
your firm is the processor, with the LLM provider as a sub-processor. The
roles cascade.

## Why this matters for engineering

The role you hold determines what you have to be able to **prove**:

- **As a controller / deployer:** lawful basis for each processing activity,
  records of processing (GDPR Art. 30), DPIA where required (Art. 35), the
  decision-trace and human-oversight evidence under EU AI Act Arts. 12 + 14.
- **As a processor:** that you only processed on documented instructions,
  that you assisted the controller with subject rights, that you signed an
  Art. 28 DPA, that you flagged any sub-processors.

Regulus' audit trail records the right things for both roles:

- Per-event `actor` field for controller attribution.
- Per-event `lawful_basis` and `purpose_code` for processing justification.
- Per-event `model_id` + provider info for sub-processor lineage.
- A separate `human_oversight_status` field on AI-decision events for AI Act
  Art. 14.

## Common confusions

- **"Joint controllers"** — when two parties jointly determine purposes and
  means (GDPR Art. 26). Multi-org A2A deployments can stumble into this if
  agents in different firms collaborate on a decision; document the
  arrangement in your DPA.
- **"Controller-to-controller"** vs. **"controller-to-processor"** transfers
  — different SCCs apply (EU SCCs Module 1 vs Modules 2/3). Affects the
  paperwork, not the engineering.
- **The deployer ≠ the provider** under the EU AI Act. If you build an agent
  on top of Gemini, Google is the *provider* of the foundation model;
  you're the *deployer* of the AI system. Deployer obligations are lighter
  than provider obligations, but only for systems that aren't high-risk.

## Next

- [Security model](security-model.md) — these roles are encoded as `Claims` on every `Identity`.
- [Risk tiers](risk-tiers.md) — provider vs. deployer obligations split by
  risk tier.
- [Audit trails](audit-trails.md) — fields that map to each role.
