package com.neullabs.regulus.grc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdapterHealthCheckTest {

    @Test
    void propagatesUnderlyingFailureWithVendorId() {
        GrcEvidenceAdapter failing = new GrcEvidenceAdapter() {
            @Override public String vendorId() { return "failing-vendor"; }
            @Override public void emit(GrcEvidenceEnvelope envelope) { }
            @Override public void healthCheck() {
                throw new IllegalStateException("DNS resolution failed");
            }
        };

        assertThatThrownBy(() -> AdapterHealthCheck.verify(List.of(failing)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failing-vendor")
                .hasMessageContaining("health check failed");
    }

    @Test
    void healthyAdaptersSilentlyPass() {
        GrcEvidenceAdapter healthy = new GrcEvidenceAdapter() {
            @Override public String vendorId() { return "healthy"; }
            @Override public void emit(GrcEvidenceEnvelope envelope) { }
            @Override public void healthCheck() { /* no-op */ }
        };
        AdapterHealthCheck.verify(List.of(healthy));
    }

    @Test
    void multipleAdaptersAllChecked() {
        // First adapter passes; second fails. The check stops at the failure.
        GrcEvidenceAdapter healthy = new GrcEvidenceAdapter() {
            @Override public String vendorId() { return "first"; }
            @Override public void emit(GrcEvidenceEnvelope envelope) { }
            @Override public void healthCheck() { /* no-op */ }
        };
        GrcEvidenceAdapter failing = new GrcEvidenceAdapter() {
            @Override public String vendorId() { return "second"; }
            @Override public void emit(GrcEvidenceEnvelope envelope) { }
            @Override public void healthCheck() { throw new RuntimeException("bad"); }
        };
        assertThatThrownBy(() -> AdapterHealthCheck.verify(List.of(healthy, failing)))
                .hasMessageContaining("second");
    }
}
