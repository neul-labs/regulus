package com.regulus.platform.governance;

import com.regulus.platform.governance.framework.Iso23053Framework;
import com.regulus.platform.governance.framework.Iso23894Framework;
import com.regulus.platform.governance.framework.Iso42001Framework;
import com.regulus.platform.governance.framework.NistAiRmfAgentInteropProfile;
import com.regulus.platform.governance.framework.NistAiRmfFramework;
import com.regulus.platform.governance.framework.NistAiRmfGenAiProfile;

import java.util.List;
import java.util.Map;

/**
 * Static catalogue of shipped governance frameworks.
 *
 * <p>Used by the Spring auto-configuration to resolve
 * {@code regulus.governance.frameworks: [...]} YAML lists and by the
 * coverage-matrix renderer.
 */
public final class GovernanceFrameworks {

    private static final Map<String, GovernanceFramework> CATALOGUE = Map.ofEntries(
            Map.entry("nist-ai-rmf", new NistAiRmfFramework()),
            Map.entry("nist-ai-rmf-600-1", new NistAiRmfGenAiProfile()),
            Map.entry("nist-ai-rmf-agent-interop", new NistAiRmfAgentInteropProfile()),
            Map.entry("iso-42001", new Iso42001Framework()),
            Map.entry("iso-23894", new Iso23894Framework()),
            Map.entry("iso-23053", new Iso23053Framework())
    );

    private GovernanceFrameworks() {}

    public static GovernanceFramework byId(String id) {
        GovernanceFramework f = CATALOGUE.get(id);
        if (f == null) {
            throw new IllegalArgumentException(
                    "Unknown governance framework: " + id + ". Known: " + CATALOGUE.keySet());
        }
        return f;
    }

    public static GovernanceFramework compose(List<String> ids) {
        return new CompositeGovernanceFramework(ids.stream().map(GovernanceFrameworks::byId).toList());
    }

    public static Map<String, GovernanceFramework> all() {
        return CATALOGUE;
    }
}
