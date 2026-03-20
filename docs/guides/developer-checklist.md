# Developer Checklist

Use this checklist when building a new Regulus-powered agent on top of the Google ADK. Each item links to deeper guidance so you can move from prototype to production without missing governance or safety steps.

## 1. Scope & Design

- [ ] Confirm business objective, allowed outcomes, and escalation rules.
- [ ] Record LEI/purpose/consent requirements with risk stakeholders (`../governance/governance-security.md`).
- [ ] Identify data sources, existing RBAC roles, and any new scopes needed (`../references/integration-matrix.md` – Identity & Secrets).
- [ ] Engage model/agent safety leads; agree initial eval thresholds and scenario coverage (`../governance/governance-security.md`).

## 2. Scaffold & Configure

- [ ] Generate project via Spring Initializr blueprint or starter archetype (`../../README.md` Quick Start).
- [ ] Add `@EnableAiAgents`, `@ModelArtefact`, and starter dependencies.
- [ ] Author pipeline in Kotlin/YAML DSL, including retrieval, tools, policy guards, planner, and optional safety classifiers (`./pipeline-dsl.md`).
- [ ] Configure RBAC/OAuth/mTLS endpoints for MCP/A2A connectors (`../references/integration-matrix.md` – OAuth/mTLS; `../references/implementation-playbooks.md` section 7).
- [ ] Define privacy selectors and retention metadata for prompts, MCP, and A2A payloads (`../governance/governance-security.md` Privacy section).

## 3. Local Development

- [ ] Use DevContainer/`make` targets to run stubs (LLM, vector DB, eval service, OTEL) (`./developer-experience.md`).
- [ ] Implement tools and planners; add safety classifier tool if required (`./pipeline-dsl.md`, `../references/implementation-playbooks.md` section 5).
- [ ] Validate DSL with `./gradlew regulusDslCheck` (or equivalent).
- [ ] Write unit/context tests covering policy gates and tool wiring.
- [ ] Exercise kill switch locally using ConfigHub/Vault stub; ensure controlled failure response (`../references/implementation-playbooks.md` section 3).

## 4. Governance & Safety Integration

- [ ] Register agent metadata with the enterprise model inventory (`../references/implementation-playbooks.md` section 1).
- [ ] Deploy/secure eval & red-team service; annotate key paths with `@AiGate` thresholds (`./hybrid-python-evals.md`, playbook section 4).
- [ ] Populate scenario catalog for safety reviews; include Consumer Duty and vulnerability cases (`../governance/risk-simulation.md`).
- [ ] Stand up safety model monitoring (precision/recall, drift) and record governance artefacts (`../references/implementation-playbooks.md` section 5).
- [ ] Configure kill switch adapters, ServiceNow change hooks, and dual-control roster (`../governance/kill-switch.md`, playbook section 3).
- [ ] Publish vendor registry entries for MCP/A2A/LLM providers (`../governance/governance-security.md` Outsourcing section, playbook section 6).
- [ ] Wire observability exporters and audit event publishers; confirm dashboards with SRE (`../references/implementation-playbooks.md` section 8).

## 5. CI/CD & Testing

- [ ] Enable Gradle tasks for DSL validation, eval checks, and risk simulation (`../references/implementation-playbooks.md` sections 2, 4, 5).
- [ ] Upload CI artefacts to GRC repository via evidence uploader (`../references/implementation-playbooks.md` section 2).
- [ ] Cut ServiceNow change/incident tickets automatically for eval failures and kill switch events (`../references/integration-matrix.md` – ServiceNow).
- [ ] Track metrics (eval pass rates, policy violations, kill switch activations) in OTEL/Grafana dashboards.

## 6. Pilot & Production Readiness

- [ ] Run `regulusRiskSim` on historical and extreme scenarios; remediate breach findings (`../governance/risk-simulation.md`).
- [ ] Present evidence pack (model inventory IDs, eval reports, scenario outcomes, vendor registry updates) to governance forums.
- [ ] Execute kill switch, escalation, and eval failure drills in staging; log outcomes.
- [ ] Finalise runbooks for kill switch, eval incident response, safety model retraining, and vendor updates (`../references/implementation-playbooks.md`).
- [ ] Obtain Model Risk, Privacy, Operational Resilience approvals; schedule launch checklist with SRE and safety teams.

> Keep the checklist in version control with your service; update items when bank-specific requirements evolve.
