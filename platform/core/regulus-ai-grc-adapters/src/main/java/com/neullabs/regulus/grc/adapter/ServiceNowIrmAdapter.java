package com.neullabs.regulus.grc.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neullabs.regulus.grc.GrcEvidenceAdapter;
import com.neullabs.regulus.grc.GrcEvidenceEnvelope;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Posts evidence into ServiceNow IRM's control-evidence table.
 *
 * <p>Default endpoint: {@code /api/now/table/sn_grc_control_evidence}.
 * Authentication: basic (username + password) or OAuth2 access token.
 *
 * <p>Field mappings reflect ServiceNow's default IRM schema; override via
 * {@link #fieldMappings} for tenant-customised schemas.
 *
 * <p>The exact field shape of ServiceNow IRM varies by tenant
 * configuration; the defaults below are the documented columns of the
 * stock {@code sn_grc_control_evidence} table. Adopters with extended
 * schemas should pass a custom {@code fieldMappings} map.
 */
public final class ServiceNowIrmAdapter implements GrcEvidenceAdapter {

    public static final Map<String, String> DEFAULT_FIELD_MAPPINGS = Map.ofEntries(
            Map.entry("eventId", "u_external_id"),
            Map.entry("occurredAt", "u_collected_at"),
            Map.entry("controlFrameworkId", "u_framework"),
            Map.entry("controlId", "u_control"),
            Map.entry("kind", "u_evidence_type"),
            Map.entry("actor", "u_collected_by"),
            Map.entry("result", "u_state"),
            Map.entry("auditEventLink", "u_source_link")
    );

    private final URI baseUri;
    private final String authHeader;
    private final Map<String, String> fieldMappings;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public ServiceNowIrmAdapter(URI baseUri, String username, String password,
                                Map<String, String> fieldMappings) {
        this.baseUri = baseUri;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes());
        this.fieldMappings = fieldMappings != null ? fieldMappings : DEFAULT_FIELD_MAPPINGS;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** OAuth2 bearer-token constructor. */
    public ServiceNowIrmAdapter(URI baseUri, String bearerToken,
                                Map<String, String> fieldMappings) {
        this.baseUri = baseUri;
        this.authHeader = "Bearer " + bearerToken;
        this.fieldMappings = fieldMappings != null ? fieldMappings : DEFAULT_FIELD_MAPPINGS;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override public String vendorId() { return "servicenow-irm"; }

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
            HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/now/table/sn_grc_control_evidence"))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "ServiceNow IRM returned " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to emit evidence to ServiceNow IRM", e);
        }
    }
}
