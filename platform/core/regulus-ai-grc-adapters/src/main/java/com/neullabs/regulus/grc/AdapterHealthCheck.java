package com.neullabs.regulus.grc;

import java.util.List;

/**
 * Startup health check invoked by the Spring auto-configuration before
 * the ADK {@code App} activates. Fail-loud if any configured adapter
 * can't reach its target — consistent with the residency-by-construction
 * principle from ADR-008.
 */
public final class AdapterHealthCheck {

    private AdapterHealthCheck() {}

    public static void verify(List<GrcEvidenceAdapter> adapters) {
        for (GrcEvidenceAdapter adapter : adapters) {
            try {
                adapter.healthCheck();
            } catch (Exception e) {
                throw new IllegalStateException(
                        "GRC adapter health check failed: vendor=" + adapter.vendorId(), e);
            }
        }
    }
}
