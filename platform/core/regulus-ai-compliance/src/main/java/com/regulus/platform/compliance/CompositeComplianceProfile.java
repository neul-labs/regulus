package com.regulus.platform.compliance;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Composes multiple {@link ComplianceProfile}s into a single effective
 * configuration. The composite always picks the *stricter* of any conflicting
 * requirements:
 *
 * <ul>
 *   <li>retention windows: the longest wins;</li>
 *   <li>residency: the intersection of allowed regions;</li>
 *   <li>CMEK / SCC requirements: any profile requiring them flips the flag on;</li>
 *   <li>audit schema: the union of required fields;</li>
 *   <li>immutability: the strongest hint wins
 *       (SIGNED &gt; MONOTONIC &gt; BEST_EFFORT).</li>
 * </ul>
 *
 * <p>The composite is what Regulus plugins / services actually consume —
 * individual profiles are inputs, not runtime configuration.
 */
public final class CompositeComplianceProfile implements ComplianceProfile {

    private final List<ComplianceProfile> profiles;

    public CompositeComplianceProfile(List<ComplianceProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("Composite requires at least one profile");
        }
        this.profiles = List.copyOf(profiles);
    }

    @Override public String id() {
        return profiles.stream().map(ComplianceProfile::id).reduce((a, b) -> a + "+" + b).orElseThrow();
    }

    @Override public String displayName() {
        return profiles.stream().map(ComplianceProfile::displayName).reduce((a, b) -> a + " + " + b).orElseThrow();
    }

    @Override public Jurisdiction jurisdiction() {
        boolean eu = profiles.stream().anyMatch(p -> p.jurisdiction() == Jurisdiction.EU || p.jurisdiction() == Jurisdiction.EU_UK);
        boolean uk = profiles.stream().anyMatch(p -> p.jurisdiction() == Jurisdiction.UK || p.jurisdiction() == Jurisdiction.EU_UK);
        if (eu && uk) return Jurisdiction.EU_UK;
        return eu ? Jurisdiction.EU : Jurisdiction.UK;
    }

    @Override public String citation() {
        return profiles.stream().map(ComplianceProfile::citation).reduce((a, b) -> a + "; " + b).orElseThrow();
    }

    @Override public Set<ControlBinding> controls() {
        Set<ControlBinding> out = new HashSet<>();
        profiles.forEach(p -> out.addAll(p.controls()));
        return Set.copyOf(out);
    }

    @Override public EventCompactionPolicy retention() {
        Duration full = profiles.stream().map(p -> p.retention().fullEventRetention()).max(Duration::compareTo).orElse(Duration.ZERO);
        Duration summary = profiles.stream().map(p -> p.retention().summaryRetention()).max(Duration::compareTo).orElse(Duration.ZERO);
        boolean erasure = profiles.stream().allMatch(p -> p.retention().erasureSupported());
        return new EventCompactionPolicy(full, summary, erasure);
    }

    @Override public ResidencyPolicy residency() {
        Set<String> intersection = null;
        for (ComplianceProfile p : profiles) {
            Set<String> regions = p.residency().allowedRegions();
            if (regions.isEmpty()) continue;
            if (intersection == null) intersection = new HashSet<>(regions);
            else intersection.retainAll(regions);
        }
        Set<String> regions = intersection == null ? Set.of() : Set.copyOf(intersection);
        boolean cmek = profiles.stream().anyMatch(p -> p.residency().requireCmek());
        ResidencyPolicy.CrossBorderTransfer xfer = profiles.stream()
                .map(p -> p.residency().crossBorderTransfer())
                .min((a, b) -> Integer.compare(a.ordinal(), b.ordinal()))
                .orElse(ResidencyPolicy.CrossBorderTransfer.FORBIDDEN);
        return new ResidencyPolicy(regions, cmek, xfer);
    }

    @Override public AuditSchema auditSchema() {
        Set<String> fields = new HashSet<>();
        profiles.forEach(p -> fields.addAll(p.auditSchema().requiredFields()));
        AuditSchema.Immutability immutability = profiles.stream()
                .map(p -> p.auditSchema().immutabilityHint())
                .max((a, b) -> Integer.compare(a.ordinal(), b.ordinal()))
                .orElse(AuditSchema.Immutability.BEST_EFFORT);
        boolean linking = profiles.stream().anyMatch(p -> p.auditSchema().subjectLinking());
        return new AuditSchema(Set.copyOf(fields), immutability, linking);
    }

    public List<ComplianceProfile> components() {
        return profiles;
    }
}
