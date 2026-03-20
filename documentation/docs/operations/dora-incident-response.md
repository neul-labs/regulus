# DORA incident response

Operating the agent's incident lifecycle under DORA Arts. 17–23.

## Triggers

A "significant" incident (per the Commission's RTS thresholds) means:

- Customer impact above the materiality bar.
- Service unavailability beyond defined RTO.
- Data integrity / confidentiality breach.

For an AI agent, "significant" can mean the model is producing systemically
wrong outputs, the audit pipeline is silently failing, the residency check
has drifted, or a third party (the LLM provider) reports an incident
upstream.

## Lifecycle

| Stage | Clock | Action |
|---|---|---|
| Detect | T=0 | Audit / alerting flags an incident |
| Classify | T+0 to T+24h | Apply RTS severity criteria |
| Early warning | T+24h | If significant, notify competent authority |
| Initial report | T+72h | Detailed report |
| Final report | T+1 month | Root cause, impact, corrective actions |

## Detection

- Audit pipeline emits events with `incident_severity` populated when
  thresholds trip. Configure alerting on the topic.
- Observability spike alerts (latency, error rate, drift) cross-correlate
  with audit signals.
- Kill switch activations are first-class incident triggers.

## Classification

Use the Commission's RTS taxonomy. Capture:

- Impact category (financial, customer, reputational, regulatory).
- Affected services.
- Affected geography.
- Duration.

Tag the audit events with the classification so the trail reconstructs.

## Notification

Most firms maintain a dedicated submission channel to the competent
authority. Regulus' audit pipeline can produce an RTS-shaped JSON body
from the relevant event window — populate the form / API from that.

## Final report

After a month, write up:

- Root cause.
- Sequence of events (reconstructed from the audit log).
- Impact (financial, customer, regulatory).
- Corrective actions (and their proof-of-completion).
- Lessons learned.

## Drill cadence

- Tabletop quarterly.
- Live drill annually — actually trip the synthetic event, follow the
  process, generate (but don't send) the early-warning record.
- Cross-org drill where third parties (LLM provider, cloud provider) are
  in scope.
