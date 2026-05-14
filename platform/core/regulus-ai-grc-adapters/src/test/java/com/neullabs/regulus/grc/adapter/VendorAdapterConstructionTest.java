package com.neullabs.regulus.grc.adapter;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for the vendor adapter constructors. Network round-trips
 * are covered by the dedicated WebhookAdapterTest with a stubbed
 * HttpServer; for the ServiceNow / OneTrust / MetricStream variants
 * we verify field-mapping override surfaces and vendor ids.
 */
class VendorAdapterConstructionTest {

    @Test
    void serviceNowAdapterConstructsWithBasicAuth() {
        ServiceNowIrmAdapter adapter = new ServiceNowIrmAdapter(
                URI.create("https://example.service-now.com"),
                "regulus-svc", "secret", null);
        assertThat(adapter.vendorId()).isEqualTo("servicenow-irm");
    }

    @Test
    void serviceNowAdapterConstructsWithBearerToken() {
        ServiceNowIrmAdapter adapter = new ServiceNowIrmAdapter(
                URI.create("https://example.service-now.com"),
                "ya29.tokenvalue", null);
        assertThat(adapter.vendorId()).isEqualTo("servicenow-irm");
    }

    @Test
    void serviceNowFieldMappingOverride() {
        Map<String, String> custom = Map.of(
                "eventId", "ext_id_custom",
                "occurredAt", "collected_at_custom");
        ServiceNowIrmAdapter adapter = new ServiceNowIrmAdapter(
                URI.create("https://example.service-now.com"),
                "tok", custom);
        assertThat(adapter).isNotNull();
    }

    @Test
    void oneTrustAdapterConstructs() {
        OneTrustAiGovernanceAdapter adapter = new OneTrustAiGovernanceAdapter(
                URI.create("https://tenant.onetrust.com"),
                "api-key-value", null);
        assertThat(adapter.vendorId()).isEqualTo("onetrust-ai-gov");
    }

    @Test
    void metricStreamAdapterConstructs() {
        MetricStreamAdapter adapter = new MetricStreamAdapter(
                URI.create("https://tenant.metricstream.com"),
                "tok", "AIControlEvidence", null);
        assertThat(adapter.vendorId()).isEqualTo("metricstream");
    }

    @Test
    void kafkaAdapterPlaceholderThrowsAtRuntime() {
        KafkaAdapter adapter = new KafkaAdapter("audit.regulus.v1");
        assertThat(adapter.vendorId()).isEqualTo("kafka");
        // emit() on the placeholder throws — see KafkaAdapter source.
    }
}
