# MetricStream adapter

MetricStream is the most-customised of the major GRC platforms. Adapter
defaults are best-effort; production deployments almost always provide
`fieldMappings`.

## Configuration

```yaml
regulus:
  grc:
    metricstream:
      enabled: true
      base-uri: https://<tenant>.metricstream.com
      auth-token: ${METRICSTREAM_TOKEN}
      intake-app-name: AIControlEvidence    # tenant-specific
```

## Endpoint

Default: `POST /api/v1/intake/{intakeAppName}/evidence`.

MetricStream organises around "applications" (mini-apps) per tenant.
Your AI evidence likely targets a tenant-specific app — set
`intake-app-name` accordingly.

## Default field mapping

| GrcEvidenceEnvelope field | MetricStream field |
|---|---|
| `eventId` | `EvidenceID` |
| `occurredAt` | `CollectedDate` |
| `controlFrameworkId` | `Framework` |
| `controlId` | `ControlID` |
| `kind` | `EvidenceType` |
| `actor` | `CollectedBy` |
| `result` | `Status` |
| `auditEventLink` | `SourceURL` |

For tenants that have custom mini-apps with custom field names, supply a
`fieldMappings` override.

## Pre-requisites

- A mini-app (intake target) defined to receive evidence.
- A service-account user with intake permissions.
- API access enabled on the tenant.

## Caveats

- **Schema drift between mini-apps.** A single MetricStream tenant can
  have many mini-apps; Regulus emits to one. For multi-app deployments,
  register multiple `MetricStreamAdapter` beans with distinct
  `intakeAppName`s and `fieldMappings`.
