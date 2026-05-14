# Generic webhook adapter

For GRC tools without a dedicated Regulus adapter — LogicGate,
Riskonnect, RSA Archer, IBM OpenPages, or your internal bespoke
intake — use the generic webhook.

## Configuration

```yaml
regulus:
  grc:
    webhook:
      enabled: true
      endpoint: https://grc.internal/regulus/evidence
      hmac-key-hex: ${REGULUS_WEBHOOK_HMAC_KEY}
```

`hmac-key-hex` is the hex-encoded 32-byte secret shared with the
receiver. Generate via `openssl rand -hex 32`.

## What the webhook receives

POST with `Content-Type: application/json`, body is the
`GrcEvidenceEnvelope` serialised as JSON, plus:

```
X-Regulus-Signature: sha256=<hex>
```

The signature is `HMAC-SHA256(body, hmac_key)`. The receiver should:

1. Reject any request without the signature header.
2. Recompute the HMAC against the raw body and compare in constant time.
3. Reject any request with a stale `occurred_at` (replay protection).

## Sample receiver (any language)

```python
import hmac, hashlib

def verify(request_body: bytes, header_signature: str, key: bytes) -> bool:
    expected = "sha256=" + hmac.new(key, request_body, hashlib.sha256).hexdigest()
    return hmac.compare_digest(expected, header_signature)
```

## What the receiver does next

That's your problem — Regulus stops at delivery. Typical pipelines:

- Map fields to the GRC tool's intake schema, then POST onward.
- Land in S3 / GCS as JSON Lines for batch ingestion.
- Forward to Kafka if your GRC tool consumes Kafka.

## When to use the webhook over a vendor adapter

- Your GRC tool isn't ServiceNow / OneTrust / MetricStream.
- You're prototyping; want a quick capture without picking a vendor yet.
- Your tenant's schema is so customised that a vendor adapter would
  spend half its time mapping anyway.
- You're consuming evidence in a bespoke internal pipeline.

## Caveats

- **No retry built into the webhook adapter.** Wrap with Resilience4j
  retry or land on a queue first if your receiver might be flaky.
- **HMAC verification is on you.** Skipping it means an attacker with
  network access can forge evidence. Don't skip it.
