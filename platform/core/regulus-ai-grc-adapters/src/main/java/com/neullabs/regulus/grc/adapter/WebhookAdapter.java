package com.neullabs.regulus.grc.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neullabs.regulus.grc.GrcEvidenceAdapter;
import com.neullabs.regulus.grc.GrcEvidenceEnvelope;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Generic JSON webhook adapter. Posts {@link GrcEvidenceEnvelope}s to an
 * arbitrary endpoint with an HMAC-SHA256 signature in the
 * {@code X-Regulus-Signature} header so the receiver can verify
 * provenance.
 *
 * <p>Use this for: LogicGate, Riskonnect, RSA Archer, IBM OpenPages, or a
 * bespoke internal GRC pipeline. Vendor-specific adapters in this module
 * are thin wrappers over this transport.
 */
public final class WebhookAdapter implements GrcEvidenceAdapter {

    private final URI endpoint;
    private final byte[] hmacKey;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public WebhookAdapter(URI endpoint, byte[] hmacKey) {
        this.endpoint = endpoint;
        this.hmacKey = hmacKey.clone();
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override public String vendorId() { return "webhook"; }

    @Override
    public void emit(GrcEvidenceEnvelope envelope) {
        try {
            byte[] body = mapper.writeValueAsBytes(envelope);
            String signature = sign(body);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .header("Content-Type", "application/json")
                    .header("X-Regulus-Signature", "sha256=" + signature)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "Webhook " + endpoint + " returned " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to emit evidence to " + endpoint, e);
        }
    }

    private String sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Override
    public void healthCheck() {
        try {
            HttpRequest probe = HttpRequest.newBuilder(endpoint)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<Void> response = http.send(probe, HttpResponse.BodyHandlers.discarding());
            int sc = response.statusCode();
            // 2xx / 4xx both mean "reachable"; only treat connection errors as unhealthy.
            if (sc >= 500) {
                throw new IllegalStateException("Webhook health check failed: " + endpoint + " returned " + sc);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Webhook health check failed for " + endpoint, e);
        }
    }
}
