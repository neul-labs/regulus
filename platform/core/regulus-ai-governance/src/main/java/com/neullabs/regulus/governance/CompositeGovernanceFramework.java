package com.neullabs.regulus.governance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Composes multiple {@link GovernanceFramework}s into a single effective
 * framework. The union of controls and bindings is exposed; no conflict
 * resolution is needed because each framework's control ids are scoped to
 * the framework.
 */
public final class CompositeGovernanceFramework implements GovernanceFramework {

    private final List<GovernanceFramework> frameworks;

    public CompositeGovernanceFramework(List<GovernanceFramework> frameworks) {
        if (frameworks == null || frameworks.isEmpty()) {
            throw new IllegalArgumentException("Composite requires at least one framework");
        }
        this.frameworks = List.copyOf(frameworks);
    }

    @Override public String id() {
        return frameworks.stream().map(GovernanceFramework::id).reduce((a, b) -> a + "+" + b).orElseThrow();
    }

    @Override public String displayName() {
        return frameworks.stream().map(GovernanceFramework::displayName).reduce((a, b) -> a + " + " + b).orElseThrow();
    }

    @Override public String version() {
        return frameworks.stream().map(GovernanceFramework::version).reduce((a, b) -> a + " | " + b).orElseThrow();
    }

    @Override public FrameworkKind kind() {
        // Prefer the strongest kind in the composite (CERTIFIABLE > STANDARD > VOLUNTARY)
        return frameworks.stream()
                .map(GovernanceFramework::kind)
                .max((a, b) -> Integer.compare(weight(a), weight(b)))
                .orElseThrow();
    }

    private static int weight(FrameworkKind k) {
        return switch (k) {
            case CERTIFIABLE -> 3;
            case STANDARD    -> 2;
            case VOLUNTARY   -> 1;
        };
    }

    @Override public Set<FrameworkControl> controls() {
        Set<FrameworkControl> out = new HashSet<>();
        frameworks.forEach(f -> out.addAll(f.controls()));
        return Set.copyOf(out);
    }

    @Override public Set<FrameworkBinding> bindings() {
        Set<FrameworkBinding> out = new HashSet<>();
        frameworks.forEach(f -> out.addAll(f.bindings()));
        return Set.copyOf(out);
    }

    @Override public String authorityUrl() {
        return frameworks.stream().map(GovernanceFramework::authorityUrl)
                .reduce((a, b) -> a + " ; " + b).orElseThrow();
    }

    public List<GovernanceFramework> components() {
        return frameworks;
    }
}
