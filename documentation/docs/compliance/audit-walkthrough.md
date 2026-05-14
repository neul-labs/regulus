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

---

## What 2L and 3L asked in parallel

The external assessor wasn't the only audience for the same week's
evidence. The bank's own **second line of defence** (risk + compliance)
and **third line** (internal audit) were running their own reviews.
Different audience, same Regulus substrate.

### 2L's questions (Head of Model Risk)

1. **"Show me the SoA refresh status."** Pulled
   `StatementOfApplicability` rendered from the production tenant's
   `GovernanceProgramState`. Every Annex A control row had a justified
   status; the 4 `PARTIAL` rows had remediation tickets linked.
2. **"What's in ServiceNow that I can attest from?"** The
   `RegulusGovernanceEvidencePlugin` had populated the IRM
   control-evidence table for the last 90 days with one row per audit
   event × matching framework binding. The 2L analyst attested 18
   controls in a sitting.
3. **"Have any control bindings drifted?"** `regulusComplianceMatrix`
   run in CI vs the checked-in matrix — no drift since the last 2L
   review.
4. **"Which `grc-adapter-failure` events fired this quarter?"** Three.
   All MetricStream pipeline blips, none missed-evidence — the
   downstream replay caught them via the Kafka audit topic.

### 3L's questions (Internal Audit Manager)

1. **"Can you reproduce a control walk-through six months from now?"**
   Yes — audit events retained 5 years raw / 7 summary; signed
   immutability under `pra-ss1-23`; SoA snapshot archived.
2. **"Demonstrate that 1L and 2L can't tamper with the trail."**
   Showed the Kafka topic ACLs (1L: produce only; 2L: read only; 3L:
   read + replay; no one with delete). HMAC signing on
   `WebhookAdapter` for the bespoke audit-archive receiver.
3. **"Sample 5 random invocations and verify the controls fired."**
   Pulled by `subject_id`; each had policy + privacy + audit events
   with the expected `framework_control_id` set.

### What this changes

Same week, same evidence, three different audiences. Each got what they
needed from the same Regulus emission. The cost-per-audience drops
sharply once the pipeline is wired — which is the whole point of
running governance + GRC as one substrate.
