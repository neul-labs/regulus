# RegulusGcsArtifactService

## In one sentence

A drop-in replacement for ADK's `GcsArtifactService` that enforces bucket
residency and CMEK at construction time and tags sensitive artifacts in
the audit trail.

## What it does

- **Bucket residency.** Constructor inspects the bucket location and
  refuses to start if it isn't on the allowlist.
- **CMEK enforcement.** Refuses to start when the profile requires CMEK
  and no key is configured.
- **Sensitive-artifact tagging.** Optional tags on uploads (e.g.
  `pii=true`) flow into audit events for the artifact lifecycle.

## When to use it

When artifacts (transcripts, generated documents, screenshots) need to be
stored with residency and key controls — i.e. always under any regulated
profile.

## Code

```java
RegulusGcsArtifactService artifacts = RegulusGcsArtifactService.wrap(
    "my-tenant-artifacts",
    "europe-west2",
    cmekKeyName,
    profile.residency());
```

## Failure modes

- Multi-region bucket (`eu` or `us`) when residency demands a single
  region → constructor throws.
- Bucket exists in a different region than configured → caught when the
  GCS client reads the bucket metadata at first use.

## See also

- [`RegulusDataResidencyPlugin`](../plugins/data-residency.md)
- [Concepts → Data residency](../concepts/data-residency.md)
