# Hybrid Python Interop & Evaluations

Regulus embraces Python where it provides the richest ecosystem—evals, red-teaming, fine-tuning—while keeping production services purely JVM. This hybrid approach keeps compliance simple and avoids shipping Python runtimes into banking estates.

## Evals & Red-Teaming Service

- **Container**: `ghcr.io/regulus/ai-evals:latest` bundles RAGAS, DeepEval, guardrails, jailbreak suites, and toxicity checks.
- **Integration**: The `regulus-ai-evals-client-starter` exposes annotations like `@AiGate(minFaithfulness=0.82, maxToxicity=0.01)` to wrap agent calls.
- **CI/CD**: A Gradle plugin stage runs eval suites on pull requests and canary traffic; builds fail when thresholds fall below policy.
- **Operations**: Platform teams host the container inside bank infrastructure, leveraging restricted network zones and existing observability.

## Fine-Tuning & Experiments

- Python workflows (Airflow, Prefect, SageMaker, etc.) remain the venue for training, fine-tuning, or experimentation.
- Outputs are delivered as hosted model endpoints or portable artefacts (ONNX, GGUF) that Java services load via DJL or HTTP.
- Documentation tracks model provenance, responsible owners, and evaluation artefacts to satisfy SS1/23 model governance.

## Traffic Flow

1. A Java agent receives a request and routes it through guardrails.
2. Before or after the response is returned, the eval client asynchronously submits prompts/responses to the Python service.
3. Metrics and pass/fail signals flow back to the JVM app, influencing deployment gates and risk dashboards.

## Governance Alignment

- Eval thresholds, test suites, and review cadences are stored alongside policy metadata so governance teams can audit changes.
- DSAR and retention policies apply to eval payloads; the service enforces TTLs and anonymisation where required.

## Future Extensions

- Add red-team scenario authoring UI or DSL for non-developer stakeholders.
- Integrate with chaos testing to simulate model outages and verify SLM/LLM routing resilience.
- Publish aggregated eval metrics to risk dashboards and cost show-back tooling.
