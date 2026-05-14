# Custom profiles

How to ship your own `ComplianceProfile` for a regulation Regulus doesn't
cover yet — or a tenant-bespoke variation of one we do.

## When to build a custom profile

- A regulation Regulus doesn't ship (PCI DSS, SOX, HIPAA for US-only
  contexts, sector-specific national rules).
- A bespoke variation: e.g. "we're FCA SYSC + extra internal policies."
- An experimental profile while we figure out whether to upstream it.

## The shape

Implement `com.neullabs.regulus.compliance.ComplianceProfile`:

```java
public final class MyCustomProfile implements ComplianceProfile {
    @Override public String id() { return "my-custom"; }
    @Override public String displayName() { return "My Custom Profile"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.EU_UK; }
    @Override public String citation() { return "Internal Policy v1"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
            new ControlBinding("purpose-binding", "§2.1",
                "Internal: every agent call carries an approved purpose code."),
            new ControlBinding("audit-trail", "§3.4",
                "Internal: 7-year retention with quarterly review.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        return new EventCompactionPolicy(
            Duration.ofDays(365 * 7),
            Duration.ofDays(365 * 10),
            true);
    }

    @Override public ResidencyPolicy residency() {
        return new ResidencyPolicy(
            Set.of("europe-west2"),
            true,
            ResidencyPolicy.CrossBorderTransfer.FORBIDDEN);
    }

    @Override public AuditSchema auditSchema() {
        return new AuditSchema(
            Set.of("event_id", "occurred_at", "actor", "action", "result",
                   "internal_control_id"),
            AuditSchema.Immutability.SIGNED,
            true);
    }
}
```

## Registering

Tell `ComplianceProfiles` about it — typically via a Spring `@Bean`:

```java
@Bean
ComplianceProfile myCustomProfile() {
    return new MyCustomProfile();
}

@Bean
@Primary
ComplianceProfile regulusComplianceProfile(List<ComplianceProfile> all) {
    return new CompositeComplianceProfile(all);
}
```

## Documenting

If you publish the profile, write a page following the 12-section
template (see [ADR-009](https://github.com/neul-labs/regulus/blob/main/docs/decisions/ADR-009-regtech-as-product-docs.md)).
The Concepts pages already cover the vocabulary you'll need.

## Contributing upstream

If your profile is general enough to ship in Regulus, open a PR. We're
particularly interested in:

- Sectoral national rules outside fin-services and health.
- Non-EU/UK profiles where there's clear demand (US sectoral, APAC
  financial services).
- Updated profiles when underlying regulations amend.

See [CONTRIBUTING](https://github.com/neul-labs/regulus/blob/main/CONTRIBUTING.md).
