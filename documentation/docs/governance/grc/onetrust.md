# OneTrust AI Governance adapter

OneTrust's AI Governance module is purpose-built for AI risk and
compliance evidence — different positioning from generic IRM tools.

## Configuration

```yaml
regulus:
  grc:
    onetrust-ai-gov:
      enabled: true
      base-uri: https://<tenant>.onetrust.com
      api-key: ${ONETRUST_API_KEY}
```

API key is a long-lived token; mint per environment and store in your
secrets backend.

## Endpoint

Default: `POST /api/aigov/v1/evidence`.

## Field mapping

Default field mappings:

| GrcEvidenceEnvelope field | OneTrust field |
|---|---|
| `eventId` | `externalId` |
| `occurredAt` | `collectedAt` |
| `controlFrameworkId` | `framework` |
| `controlId` | `controlReference` |
| `kind` | `evidenceType` |
| `actor` | `collectedBy` |
| `result` | `outcome` |
| `auditEventLink` | `sourceUri` |

OneTrust supports tenant-specific custom fields; supply a
`fieldMappings` override if your tenant uses non-default field names.

## What OneTrust does with it

The adapter populates the evidence intake; OneTrust's workflow attaches
the evidence to the relevant control or risk record. From there:

- Control testing is attestable directly from OneTrust's UI.
- The Risk module surfaces AI-driven incidents with the same provenance.
- The Data Mapping module receives the privacy-relevant slice.

## Caveats

- **OneTrust + ServiceNow integration** exists separately at the
  platform level (OneTrust can populate ServiceNow data inventories).
  Regulus emits to each adapter independently — there's no need to
  forward via OneTrust.
- **AI Governance module licensing.** Customers without the AI
  Governance module need a custom field mapping to land evidence in
  the base Compliance or Risk Management module.
