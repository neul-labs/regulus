package com.neullabs.regulus.grc.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neullabs.regulus.grc.GrcEvidenceAdapter;
import com.neullabs.regulus.grc.GrcEvidenceEnvelope;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Posts evidence into OneTrust's AI Governance module.
 *
 * <p>Default endpoint: {@code /api/aigov/v1/evidence}. Authentication via
 * a long-lived API key passed as {@code X-OneTrust-API-Key}.
 *
 * <p>OneTrust's tenant-configured field set varies; the defaults below
 * reflect their documented intake schema for evidence records. Adopters
 * with customised schemas should pass a {@code fieldMappings} map.
 */
public final class OneTrustAiGovernanceAdapter implements GrcEvidenceAdapter {

    public static final Map<String, String> DEFAULT_FIELD_MAPPINGS = Map.ofEntries(
            Map.entry("eventId", "externalId"),
            Map.entry("occurredAt", "collectedAt"),
            Map.entry("controlFrameworkId", "framework"),
            Map.entry("controlId", "controlReference"),
            Map.entry("kind", "evidenceType"),
            Map.entry("actor", "collectedBy"),
            Map.entry("result", "outcome"),
            Map.entry("auditEventLink", "sourceUri")
    );

    private final URI baseUri;
    private final String apiKey;
    private final Map<String, String> fieldMappings;
    private final HttpClient http;
    private final ObjectMapper mapper = AdapterJson.mapper();

    public OneTrustAiGovernanceAdapter(URI baseUri, String apiKey,
                                       Map<String, String> fieldMappings) {
        this.baseUri = baseUri;
        this.apiKey = apiKey;
        this.fieldMappings = fieldMappings != null ? fieldMappings : DEFAULT_FIELD_MAPPINGS;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override public String vendorId() { return "onetrust-ai-gov"; }

    @Override
    public void emit(GrcEvidenceEnvelope envelope) {
        Map<String, Object> row = new HashMap<>();
        row.put(fieldMappings.get("eventId"), envelope.eventId());
        row.put(fieldMappings.get("occurredAt"), envelope.occurredAt().toString());
        row.put(fieldMappings.get("controlFrameworkId"), envelope.controlFrameworkId());
        row.put(fieldMappings.get("controlId"), envelope.controlId());
        row.put(fieldMappings.get("kind"), envelope.kind().name());
        row.put(fieldMappings.get("actor"), envelope.actor());
        row.put(fieldMappings.get("result"), envelope.result());
        if (envelope.auditEventLink() != null) {
            row.put(fieldMappings.get("auditEventLink"), envelope.auditEventLink().toString());
        }

        try {
            byte[] body = mapper.writeValueAsBytes(row);
            HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/aigov/v1/evidence"))
                    .header("X-OneTrust-API-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "OneTrust AI Governance returned " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to emit evidence to OneTrust AI Governance", e);
        }
    }
}
