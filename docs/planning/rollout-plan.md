# Rollout Plan

Regulus delivery spans twelve weeks, focusing first on scaffolding and governance, then on interoperability and resilience. Adjust dates based on team capacity and dependency readiness.

> **Note**: This rollout plan covers the initial 12-week delivery (Phase 0). For the complete platform evolution including documentation, operational maturity, and advanced features, see `./product-roadmap.md`.

## Weeks 0–2: Scaffold

- Create `adk-integration` module with Micrometer/OTEL/Kafka hooks around ADK events.
- Implement `PolicyGuard` (AOP) enforcing LEI/purpose/consent checks.
- Build `PrivacyFilter` for prompt/MCP/A2A redaction and metadata tagging.
- Ship the `KillSwitchInterceptor` with ConfigHub/Vault toggle wiring and basic alerting hooks.
- Stand up an MCP client to consume the ISO 20022 validator (mock acceptable initially).
- Publish the DevContainer, `make` targets, and JShell snippets so teams can exercise agents locally.
- Validate Gradle BOM, starters, and DSL parsers end-to-end in a sample service.

## Weeks 3–5: Expose & Interop

- Expose the ISO validator as an MCP server for cross-team reuse.
- Convert the pilot agent to an A2A server with documented contracts and RBAC scopes.
- Integrate A2A client flows that call external agents (e.g., AML).
- Harden gateway policies (OAuth scopes, mTLS) for MCP and A2A endpoints.
- Release the Spring Initializr blueprint, starter templates, and CLI/Git hook bundle for policy and DSL linting.

## Weeks 6–8: Resilience & Governance

- Implement SLM-first routing with LLM fallback; capture routing decisions for audit.
- Run chaos tests simulating LLM outages and MCP/A2A failures.
- Generate SS1/23 artefacts, outsourcing metadata (SS2/21), and operational resilience evidence (PS21/3).
- Automate eval/red-team checks in CI with gating thresholds agreed with risk stakeholders.
- Stand up the vendor/outsourcing registry module and due-diligence workflows.
- Prototype the `regulus-ai-risk-simulation` module and Gradle task with initial scenario catalog.
- Formalise the model/agent safety board: define eval thresholds, simulation sign-off cadence, and kill-switch drill participation.
- Produce implementation playbooks and confirm integration owners per `../references/integration-matrix.md`.

## Weeks 9–12: Pilot & Sign-Off

- Run pilot with representative workloads; capture metrics, incidents, and mitigations.
- Demonstrate DSAR export across ADK/MCP/A2A traces.
- Present governance artefacts to Model Risk, Privacy, and Operational Resilience forums for production approval.
- Plan broader rollout, including developer education sessions and support model.
- Operationalise the risk simulation reporting pack and schedule ongoing replay/drift runs.
- Complete kill switch runbooks, dual-control testing, and quarterly drill calendar handover.
- Execute dry run of the implementation playbooks to validate adapters, runbooks, and escalation paths.

## Cross-Cutting Considerations

- **Stakeholder Alignment**: Maintain a RACI\* covering engineering, security, risk, and business owners.
- **Documentation**: Keep docs/ updated each sprint with decisions, schemas, and onboarding guidance.
- **Metrics**: Track adoption, eval pass rates, mean time to mitigation, and model cost show-back as success indicators.

\*RACI = Responsible, Accountable, Consulted, Informed.

---

## Beyond Week 12

Upon successful completion of this 12-week rollout:

1. **Transition to Phase 1** (Documentation & Developer Experience) - see `./product-roadmap.md`
2. **Continuous Improvement**: Begin iterative enhancements based on pilot feedback
3. **Broader Adoption**: Onboard additional teams with lessons learned from pilot
4. **Operational Excellence**: Execute Phase 2 (Operational Maturity) to scale production usage

For the complete multi-phase roadmap including documentation enhancements, operational runbooks, advanced features, and continuous improvement initiatives, refer to `./product-roadmap.md`.
