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
 * Posts evidence into MetricStream's evidence intake API.
 *
 * <p>MetricStream's API surface varies more by tenant configuration than
 * the other major vendors; the defaults here reflect the most common
 * documented intake shape, and adopters typically need to bind to their
 * own custom application via {@code fieldMappings}.
 */
public final class MetricStreamAdapter implements GrcEvidenceAdapter {

    public static final Map<String, String> DEFAULT_FIELD_MAPPINGS = Map.ofEntries(
            Map.entry("eventId", "EvidenceID"),
            Map.entry("occurredAt", "CollectedDate"),
            Map.entry("controlFrameworkId", "Framework"),
            Map.entry("controlId", "ControlID"),
            Map.entry("kind", "EvidenceType"),
            Map.entry("actor", "CollectedBy"),
            Map.entry("result", "Status"),
            Map.entry("auditEventLink", "SourceURL")
    );

    private final URI baseUri;
    private final String authToken;
    private final String intakeAppName;
    private final Map<String, String> fieldMappings;
    private final HttpClient http;
    private final ObjectMapper mapper = AdapterJson.mapper();

    public MetricStreamAdapter(URI baseUri, String authToken, String intakeAppName,
                               Map<String, String> fieldMappings) {
        this.baseUri = baseUri;
        this.authToken = authToken;
        this.intakeAppName = intakeAppName;
        this.fieldMappings = fieldMappings != null ? fieldMappings : DEFAULT_FIELD_MAPPINGS;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override public String vendorId() { return "metricstream"; }

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
            HttpRequest request = HttpRequest.newBuilder(
                    baseUri.resolve("/api/v1/intake/" + intakeAppName + "/evidence"))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "MetricStream returned " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to emit evidence to MetricStream", e);
        }
    }
}
