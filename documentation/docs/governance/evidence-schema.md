# Evidence schema

The canonical `GrcEvidenceEnvelope` every adapter receives. Vendor
adapters translate this to the receiver's schema; the webhook adapter
serialises it directly.

## JSON shape

```json
{
  "event_id": "01J6X4ABCDEFG",
  "occurred_at": "2026-05-14T11:23:09.123Z",
  "control_framework_id": "iso-42001",
  "control_id": "A.7.3",
  "compliance_profile_id": "uk-gdpr",
  "regulation_clause": "Art. 25",
  "kind": "CONTROL_TEST",
  "actor": "user:42",
  "result": "pass",
  "attributes": {
    "mechanism": "pii-redaction",
    "subject_id": "subj-9001",
    "redactions": ["NINO_1"],
    "model_id": "gemini-2.5-flash",
    "ai_act_risk_tier": "limited"
  },
  "audit_event_link": "regulus-audit://01J6X4ABCDEFG"
}
```

## Field semantics

| Field | Type | Notes |
|---|---|---|
| `event_id` | string (ULID) | Same value as the source audit event |
| `occurred_at` | ISO-8601 instant in UTC | Source audit event's timestamp |
| `control_framework_id` | string | Framework profile id, e.g. `nist-ai-rmf`, `iso-42001` |
| `control_id` | string | Framework control id, e.g. `GOVERN-1.5`, `A.6.2.7` |
| `compliance_profile_id` | string \| null | Regulation profile id when the mechanism is also bound to a regulation |
| `regulation_clause` | string \| null | E.g. `Art. 25`, `SYSC 13.9` |
| `kind` | enum | `CONTROL_TEST` / `INCIDENT` / `POLICY_ENFORCEMENT` / `EXCEPTION` |
| `actor` | string | E.g. `user:42`, `agent:intake-bot`, `system:scheduler` |
| `result` | string | `pass` \| `fail` \| `exception-recorded` |
| `attributes` | object | Free-form. The source audit event's full payload |
| `audit_event_link` | URI | Back-pointer to the raw audit record |

## Why a separate envelope and not "send the audit event"

- **Schema stability.** GRC tools care about the control mapping fields
  first; the raw audit event has lots of fields that don't map cleanly.
- **Multiple envelopes per event.** One audit event can satisfy multiple
  framework controls (e.g. a PII redaction satisfies GAI-4 *and* ISO
  A.7.3). One envelope per binding, not per event.
- **Vendor isolation.** Schema drift in adapters doesn't leak back into
  the audit topic.

## Adding fields per tenant

The `attributes` map is the extension point — set any tenant-specific
fields on the audit event and they ride through.

For very heavy customisation, write a Spring `@Bean` that wraps the
default `RegulusGovernanceEvidencePlugin` with your own transformer.
