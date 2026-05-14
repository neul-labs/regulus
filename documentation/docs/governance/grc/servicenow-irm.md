# ServiceNow IRM adapter

ServiceNow Integrated Risk Management — the most widely deployed GRC
platform in enterprise environments, with an AI Control Tower module
extended at Knowledge 2026.

## Configuration

```yaml
regulus:
  grc:
    servicenow-irm:
      enabled: true
      base-uri: https://<instance>.service-now.com
      bearer-token: ${SERVICENOW_OAUTH_TOKEN}      # preferred
      # or:
      # username: regulus-svc
      # password: ${SERVICENOW_PASSWORD}
```

For OAuth, mint a refreshable token via your IDP integration and pass it
through. For basic auth, prefer a dedicated service account scoped to
the IRM evidence table.

## Endpoint

Default: `POST /api/now/table/sn_grc_control_evidence`.

ServiceNow's IRM uses the `sn_grc_control_evidence` table as the
canonical store for control-test evidence. The adapter POSTs one row
per envelope.

## Field mapping

Default field mappings (override via `fieldMappings` constructor arg):

| GrcEvidenceEnvelope field | ServiceNow column |
|---|---|
| `eventId` | `u_external_id` |
| `occurredAt` | `u_collected_at` |
| `controlFrameworkId` | `u_framework` |
| `controlId` | `u_control` |
| `kind` | `u_evidence_type` |
| `actor` | `u_collected_by` |
| `result` | `u_state` |
| `auditEventLink` | `u_source_link` |

For tenants that have customised IRM with additional columns, pass a
custom `fieldMappings` map at bean construction.

## Pre-requisites in ServiceNow

- Activated **IRM** application.
- A service-account user with **`sn_grc_role_user`** or equivalent.
- Optional: AI Control Tower module installed for richer control
  taxonomy.

## What an audit event becomes

```
[Regulus]                                        [ServiceNow IRM]
event_id=01J6X4...   mechanism=pii-redaction
clause_citation=Art.25   framework_control_id=A.7.3
                                              →  u_external_id=01J6X4...
                                                 u_framework=iso-42001
                                                 u_control=A.7.3
                                                 u_evidence_type=CONTROL_TEST
                                                 u_state=pass
                                                 u_source_link=regulus-audit://01J6X4...
```

The IRM control-testing workflow then picks up the row, attaches it to
the relevant Control record, and the GRC team can attest the control
without needing direct access to the audit topic.

## Caveats

- **Tenant schema variance.** Stock IRM uses the columns above;
  custom-extended IRMs may rename them. Use `fieldMappings`.
- **Rate limits.** ServiceNow throttles per-instance; the adapter does
  not currently rate-limit. For high-volume agents, enable the
  `RegulusGovernanceEvidencePlugin` only for `EvidenceKind.INCIDENT`
  and `POLICY_ENFORCEMENT` rather than every CONTROL_TEST.
