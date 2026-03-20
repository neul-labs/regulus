# Data residency

The location of the machine processing your data is a legal question, not
just a latency one. Regulus enforces it at startup so it can't drift at
runtime.

## The legal shape

GDPR Arts. 44–49 say personal data may not move outside the EEA *unless* one
of the following applies:

- The destination has an **adequacy decision** (e.g. EU → UK adequacy
  decision; EU → US Data Privacy Framework).
- You sign **Standard Contractual Clauses (SCCs)** with the receiving party
  and do a transfer impact assessment.
- A narrow Art. 49 derogation applies (rare, brittle).

UK GDPR has the same shape; the UK uses the **International Data Transfer
Agreement (IDTA)** or the **UK Addendum to EU SCCs** instead of EU SCCs.

For UK financial services, **PRA SS2/21** adds an operational requirement:
critical data should sit on infrastructure the firm can audit and exit. NHS
DSPT goes further for confidential patient information — UK-only is the
practical default.

## The cloud-engineering shape

Cloud regions matter because each region has a fixed physical and legal
location. For GCP (the ADK reference cloud):

- `europe-west2` — London. UK personal data sits here cleanly.
- `europe-west1` (Belgium), `europe-west3` (Frankfurt), `europe-west4`
  (Eemshaven, Netherlands), etc. — EU/EEA.
- `europe-west8`, `-9`, `-12` — more recent EU regions.
- `us-central1`, `us-east1`, etc. — US. Crossing to these from EU/UK personal
  data triggers transfer rules.

For Vertex AI specifically, the **model endpoint's region** is where your
prompt is sent. Picking the wrong location for a multi-region project is the
classic GDPR footgun.

## How Regulus enforces it

`RegulusDataResidencyPlugin` runs two checks:

1. **At application startup.** It inspects the wired `SessionService`,
   `MemoryService`, `ArtifactService`, and the configured model location. If
   any sits outside the tenant's residency allowlist, the ADK `App` refuses
   to activate.
2. **Per call (defence in depth).** A `BeforeAgentCallback` re-validates the
   request's target endpoints in case anything was rewired at runtime.

Fail-closed at startup. No "we'll warn you in the logs and let production
keep running" mode.

The allowlist comes from two sources:

- The composite of your active profiles' `ResidencyPolicy.allowedRegions()`
  (intersection — strictest wins).
- An optional explicit `regulus.adk.residency.allowed-regions` in YAML.

If neither is empty, the intersection is used. If both are empty (the
"unconstrained" case), the plugin still runs but doesn't block anything —
recommended only for non-personal-data agents.

## CMEK

Customer-Managed Encryption Keys: you control the key that encrypts data at
rest, the cloud provider uses it via KMS. Some profiles (`dora`, `fca-sysc`,
`pra-ss1-23`, `pra-ss2-21`, `nhs-dspt`, `ehds`) require CMEK by default;
Regulus surfaces this as `ResidencyPolicy.requireCmek()` and refuses to
start a session or artifact service without a key configured when required.

## Companion services

Residency on the session service is one boundary. The full picture:

| Boundary | Regulus class | What it pins |
|---|---|---|
| Sessions (Vertex AI managed) | `RegulusVertexAiSessionService` | Vertex location |
| Sessions (Firestore) | `RegulusFirestoreSessionService` | Firestore DB location |
| Memory (Firestore) | `RegulusFirestoreMemoryService` | Firestore DB location |
| Artifacts (GCS) | `RegulusGcsArtifactService` | Bucket location |
| Model endpoint | Built into ADK's Vertex client; checked by `RegulusDataResidencyPlugin` at startup |

Each refuses to construct if the configured location isn't on the allowlist.

## Common pitfalls

- **Default Firestore databases sit in `nam5` (US)** unless you create a
  regional one. The `Regulus*` services catch this at startup.
- **Multi-region buckets in GCS** technically cross regions. The plugin
  treats `eu` multi-region as EU-eligible but **not** UK-only — explicit
  region required for UK-only profiles.
- **Model endpoints have to match.** Calling Gemini in `us-central1` from a
  London-pinned app is the classic mistake. The plugin's startup check
  catches it.
- **Streaming responses still flow through the model endpoint's region.**
  Regional pinning is per-call, not per-byte.

## Next

- [Dual control / 4-eyes](dual-control.md)
- [Plugin reference → RegulusDataResidencyPlugin](../plugins/data-residency.md)
