# adk-quickstart — Regulus on Google ADK in 10 minutes

A Spring Boot app that runs a Gemini-backed ADK agent with the full Regulus
compliance plane switched on. By the end of this walkthrough you will have:

- A working ADK 1.2.0 agent on Java 21 + Spring Boot 3.3.
- Six Regulus `BasePlugin`s active: policy, privacy, audit, kill switch,
  model risk, data residency.
- A composite compliance profile across **EU AI Act + UK GDPR + FCA SYSC**.
- A `europe-west2` residency pin verified at startup (fail-closed).

This codelab mirrors Google's own ADK Java quickstart in shape, so if you have
read [Google's ADK Java getting-started](https://google.github.io/adk-docs/get-started/java/),
nothing here will feel unfamiliar — you just have a controls plane added.

## Prerequisites

- Java 21
- A GCP project with the Vertex AI API enabled
- `gcloud auth application-default login` so ADK and Regulus can authenticate
- `GOOGLE_CLOUD_PROJECT` env var set

## Run it

```bash
./gradlew :examples:adk-quickstart:bootRun
```

The first lines of startup output show each Regulus plugin registering on the
ADK `App`:

```
regulus-audit:         sink=stdout, profile=eu-ai-act+uk-gdpr+fca-sysc
regulus-policy:        controls=27 across 3 profiles
regulus-privacy:       patterns=NINO,IBAN,BIC,SORT_CODE,UK_ACCOUNT_NUMBER,UK_POSTCODE,EMAIL
regulus-kill-switch:   dual-control=true, scope=quickstart-agent
regulus-data-residency: allowed=[europe-west2], cmek=false  (location verified)
regulus-model-risk:    tenantTier=STANDARD
```

## What changes when you remove Regulus

Comment out the Regulus dependencies in `build.gradle.kts` and re-run. The
agent still works — but every request now goes to the LLM unredacted, leaves
no audit trail, has no policy guards, and there is no kill switch. Same agent,
zero compliance plane. That delta is what Regulus replaces with two
dependencies and a YAML block.

## Try the controls

Hit the example endpoint (omitted from this README — see `controllers/`):

1. With a NINO in the prompt → `regulus-privacy` masks it before the LLM call;
   audit event shows `redactions: [NINO_1]`.
2. With `automated_legal_effect=true` in the headers → `regulus-policy` blocks
   under GDPR Art. 22 and surfaces the citation in the response.
3. Flip the kill switch via the actuator endpoint → next request is rejected
   with `KillSwitchActive`; deactivation requires a second operator.

## Where to read next

- [Plugin reference](../../documentation/docs/plugins/) — one page per plugin.
- [Compliance — UK GDPR](../../documentation/docs/compliance/uk/uk-gdpr.md)
- [Compliance — EU AI Act](../../documentation/docs/compliance/eu/eu-ai-act.md)
- [`adk-multi-agent-a2a`](../adk-multi-agent-a2a/README.md) — same plugins,
  two agents talking over A2A.
- [`adk-vertex-agent-engine-deploy`](../adk-vertex-agent-engine-deploy/README.md)
  — `adk deploy` to Vertex AI Agent Engine, Regulus baked in.
