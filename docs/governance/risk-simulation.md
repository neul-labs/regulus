# Risk Simulation Module

To satisfy model risk teams during initial deployment, Regulus will ship a `regulus-ai-risk-simulation` module that exercises agents in controlled environments before production go-live.

## Objectives

- Quantify expected error rates, policy breaches, and bias signals using historical data.
- Demonstrate resilience of guardrails, kill switches, and fallback strategies under stress.
- Produce auditable evidence that supports SS1/23 model approval and PS21/3 resilience expectations.

## Simulation Types

- **Historical Replay**: Run the agent against recorded transactions/interactions with known ground truth. Reports include accuracy, policy compliance, and escalation decisions vs. human outcomes.
- **Scenario Library**: Execute curated extreme-but-plausible scenarios (regulatory edge cases, missing consents, anomalous payments) defined with risk SMEs.
- **Dependency Stress Tests**: Simulate MCP/LLM outages, latency spikes, and misconfigurations to ensure kill switches, circuit breakers, and incident hooks fire correctly.
- **Shadow Mode**: In pre-production or limited production, run agents in observe-only mode; capture divergence from human actions and flag risky recommendations.
- **Monte Carlo Guardrail Probing**: Generate synthetic prompts varying policy inputs (consent flags, LEI formats, retention directives) to uncover edge cases where guardrails might fail.

## Architecture

```
┌──────────────────┐      ┌────────────────────┐
│ Scenario Catalog │──┐──▶│ Simulation Runner  │──┐
└──────────────────┘  │   └────────────────────┘  │
                      │                           ▼
┌──────────────────┐  │                  ┌──────────────────┐
│ Historical Data  │──┘                  │ Regulus Agent    │
└──────────────────┘                     │ (ADK Runtime)    │
                                         └──────────────────┘
                                                │
                                                ▼
                                         ┌──────────────────┐
                                         │ Metrics & Logs   │
                                         └──────────────────┘
                                                │
                                                ▼
                                         ┌──────────────────┐
                                         │ Risk Reports     │
                                         └──────────────────┘
```

## Outputs

- **Simulation Report**: Accuracy metrics, policy breaches, bias signals, fallback activations, and comparisons against acceptance thresholds.
- **Resilience Evidence**: Logs demonstrating kill switch triggers, circuit breaker behaviour, incident ticket creation, and recovery steps.
- **Approval Pack Integration**: Reports feed the SS1/23 model approval dossier and PS21/3 resilience evidence repository.

## Integration Points

- Gradle plugin task (`regulusRiskSim`) executes scenarios pre-release and uploads artefacts to the GRC repository.
- CLI/Console UI allows risk analysts to select scenario bundles and review results.
- Optional API for continuous simulations (e.g., nightly replays for drift detection).

## Safety Team Workflow

- **Scenario Stewardship**: The model/agent safety group prioritises and approves scenario additions, ensuring coverage of regulatory edge cases and recent incidents.
- **Gatekeeping**: Safety owners review simulation reports alongside eval metrics; releases cannot progress without their sign-off when guardrail breaches exceed tolerance.
- **Drift Watch**: Continuous or scheduled simulations feed safety dashboards; the team raises mitigation workstreams when degradation or new failure modes appear.
- **Evidence Archival**: Safety analysts coordinate with governance to deposit simulation artefacts, remediation actions, and decisions into the GRC repository for audit readiness.

## Next Steps

1. Define schema for scenario catalog and expected outputs (JSON reports, dashboards).
2. Partner with risk SMEs to curate initial historical replay datasets and scenario scripts.
3. Prototype the simulation runner using the existing eval service infrastructure, ensuring isolation from production data.
4. Document acceptance thresholds and sign-off workflows within the model governance process.
