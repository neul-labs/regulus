# Audit walkthrough

What we showed the auditor when an external assessor sat next to one of our
reference customers for a day. Names changed, structure preserved.

## Setting

A UK retail bank rolling out an FCA-authorised mortgage-advice agent. Stack:

- ADK 1.2.0, Java 21, Spring Boot 3.3
- Vertex AI Gemini 2.5 Pro, pinned to `europe-west2`, CMEK applied
- Regulus profiles: `eu-ai-act, uk-gdpr, fca-sysc, pra-ss1-23, pra-ss2-21`
- Audit to Kafka topic `audit.mortgage.v1`, 5 + 7 year retention
- Kill switch dual-control on the operations team's runbook

The audit window was a single day. The auditor came in with a checklist of
~20 questions plus open-ended exploration.

## Q1: "Walk me through how one request flows through your agent."

We pulled a live trace from the audit log for a recent customer interaction.
The event chain showed:

```
[1] before-agent       actor=user:42  purpose=mortgage-advice  kill-switch=clear
[2] before-model       model=gemini-2.5-pro  tier=REGULATED    redactions=[NINO_1, IBAN_1]
[3] after-model        latency_ms=812  output_redactions=[NINO_1]
[4] before-tool        tool=eligibility-check  policy=allow
[5] after-tool         result=eligible
[6] after-agent        consumer_duty_outcome=support  result=advice-generated
```

We highlighted that the NINO appears in the redactions list at step [2] but
never as a raw value anywhere in the trace — `RegulusPrivacyPlugin` strips
it before the model call and `AfterModelCallback` re-redacts the output.

## Q2: "Show me your model inventory."

We opened the model registry table. Each row had:

- `model_id` (e.g. `gemini-2.5-pro`).
- `version_pin` (e.g. `2026-05-01`).
- `risk_tier` (`REGULATED`).
- `validation_status` (`approved-2026-04-12`).
- `validation_evidence_ref` (link to the PDF in the model-risk SharePoint).
- `provider_lei` (Google's LEI).
- `outsourcing_register_entry` (link to SS2/21 register).
- `kill_switch_scope` (`agents/mortgage-advice`).

The auditor wanted to see the validation evidence — we walked from the
registry → audit log entry → PDF.

## Q3: "Demonstrate human oversight."

We tripped the kill switch.

- Operator A used the `/admin/kill-switch` endpoint with a synthetic
  reason. The next agent invocation came back with
  `result=kill-switch-active` and the audit emitted
  `action=kill-switch-activate, actor=ops-A, reason=audit-drill`.
- Operator B proposed deactivation. The audit emitted
  `action=kill-switch-deactivate-requested`.
- Operator A *tried to confirm* the deactivation themselves. The system
  refused: `confirming operator must differ from requester`. The audit
  recorded the refused attempt.
- Operator C (different person) confirmed. Audit:
  `action=kill-switch-deactivated, requester=ops-B, confirmer=ops-C`.

The auditor asked to see this written down somewhere — we pointed to
`Operations → Kill-switch playbook`.

## Q4: "How do you stop personal data from leaving the UK?"

We showed the residency plugin's startup behaviour:

```
$ ./gradlew :examples:adk-vertex-agent-engine-deploy:bootRun \
    -Dregulus.adk.session-service.location=us-central1
...
RegulusVertexAiSessionService refused to start: location 'us-central1'
is not in the residency allowlist [europe-west2]
```

We then showed that the production app was configured for `europe-west2`
and the model endpoint pinned to the same. The auditor noted the *fail-
closed* behaviour was satisfactory: no runtime warning that could be
ignored, no graceful degradation.

## Q5: "Show me your retention policy in action."

We pulled the Kafka topic configuration: 5 years for raw events, 7 years
for summaries. Then we showed the `RegulusRetentionEventCompactor` task
log:

```
[regulus-compaction] sweep completed: 0 events dropped, 1,243,512
events summarised (older than 5y), 0 erasures (FCA SYSC retention
overrides erasure)
```

The auditor noted the explicit "FCA SYSC retention overrides erasure" —
this is the regulator-side override we mentioned during the GDPR walk.

## Q6: "How would you handle a customer's subject access request?"

We showed the subject-linked query: `subject_id=ABC123` returns every audit
event mentioning that customer. The output is structured JSON with
redactions in place; producing a customer-facing response requires
de-pseudonymising on the way out — handled by the bank's existing SAR
process, not Regulus.

The auditor asked about Art. 17 erasure: we explained that under the
active profile composite, FCA SYSC's 5-year retention overrides the
customer's erasure right (regulator wins). For customers whose retention
window has passed, erasure is honoured and produces a tombstone.

## Q7: "What does your incident-response evidence look like?"

We ran the synthetic-incident drill: a tagged event with
`incident_severity=major`. The audit pipeline produced a NIS2-shaped early-
warning record within the 24h window (visible in the drill output, not
actually sent). The drill itself was logged.

## Q8: "Demonstrate Consumer Duty outcome tracking."

We opened the outcomes dashboard, grouped by `consumer_duty_outcome`. The
four outcomes (`products`, `price`, `understanding`, `support`) each had a
count of agent interactions. The auditor wanted to see vulnerable-customer
flagging — we sent a synthetic request with `vulnerable_customer=true`. The
audit showed `RequireConfirmation` and the ADK `ToolConfirmation` flow
firing.

## Q9: "Show me an event you couldn't emit."

This was a test — would the system fail-loud if it tried to emit an
incomplete event? We constructed a deliberately broken `EventEnvelope`
missing `purpose_code`. The audit sink rejected it; the agent invocation
failed with an explicit error citing the missing field and the active
profile. The event was *not* emitted (no half-events in the trail).

## Q10: "What's not Regulus's responsibility?"

We walked the "What this doesn't cover" sections of the FCA SYSC, PRA
SS1/23, and SS2/21 pages. Specifically:

- Negotiating contracts with Google — the bank's procurement.
- Choosing model materiality tiers — the bank's model risk function.
- Validating the model itself — independent validation team.
- Operating the dual-control runbook — the bank's operations team.

## What the auditor said at the end

> "I haven't seen this level of structural compliance in an AI deployment
> before. The thing I want to see in the post-go-live monitoring is the
> incident drill happening quarterly with rotating operators, not just
> the same two people."

That comment is now in the `Operations → Kill-switch playbook` as a
calendared task.

## What you should take from this

The audit was tractable because every question had a single artefact to
point at: a config, a YAML key, a Kafka event, a plugin's source. Regulus
doesn't pre-empt the auditor's question — it makes the answer the same
shape every time. That's the engineering value.
