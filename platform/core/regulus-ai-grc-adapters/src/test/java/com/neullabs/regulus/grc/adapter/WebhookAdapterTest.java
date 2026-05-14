package com.neullabs.regulus.grc.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neullabs.regulus.grc.EvidenceKind;
import com.neullabs.regulus.grc.GrcEvidenceEnvelope;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookAdapterTest {

    private HttpServer server;
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final List<String> signatures = new CopyOnWriteArrayList<>();
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/grc/evidence", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            bodies.add(new String(body, StandardCharsets.UTF_8));
            signatures.add(exchange.getRequestHeaders().getFirst("X-Regulus-Signature"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void emitsToTheConfiguredEndpoint() {
        byte[] hmacKey = HexFormat.of().parseHex("deadbeef".repeat(8)); // 32 bytes
        WebhookAdapter adapter = new WebhookAdapter(
                URI.create("http://127.0.0.1:" + port + "/grc/evidence"),
                hmacKey);

        adapter.emit(envelope());

        assertThat(bodies).hasSize(1);
        assertThat(signatures).hasSize(1);
        assertThat(signatures.get(0)).startsWith("sha256=");
    }

    @Test
    void hmacSignatureMatchesExternalComputation() throws Exception {
        byte[] hmacKey = HexFormat.of().parseHex("deadbeef".repeat(8));
        WebhookAdapter adapter = new WebhookAdapter(
                URI.create("http://127.0.0.1:" + port + "/grc/evidence"),
                hmacKey);

        GrcEvidenceEnvelope env = envelope();
        adapter.emit(env);

        // Recompute the HMAC from the captured body and compare.
        byte[] capturedBody = bodies.get(0).getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
        String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(capturedBody));

        assertThat(signatures.get(0)).isEqualTo(expected);
    }

    @Test
    void bodyIsValidJson() throws Exception {
        byte[] hmacKey = HexFormat.of().parseHex("deadbeef".repeat(8));
        WebhookAdapter adapter = new WebhookAdapter(
                URI.create("http://127.0.0.1:" + port + "/grc/evidence"),
                hmacKey);

        adapter.emit(envelope());

        Map<?, ?> parsed = new ObjectMapper().readValue(bodies.get(0), Map.class);
        assertThat(parsed).containsKey("eventId");
        assertThat(parsed).containsKey("controlFrameworkId");
    }

    @Test
    void vendorIdIsWebhook() {
        WebhookAdapter adapter = new WebhookAdapter(
                URI.create("http://127.0.0.1:" + port + "/grc/evidence"),
                new byte[32]);
        assertThat(adapter.vendorId()).isEqualTo("webhook");
    }

    private static GrcEvidenceEnvelope envelope() {
        return new GrcEvidenceEnvelope(
                "01J6X4ABCDEFG", Instant.parse("2026-05-14T11:23:09.123Z"),
                "iso-42001", "A.7.3",
                "uk-gdpr", "Art. 25",
                EvidenceKind.CONTROL_TEST,
                "user:1", "pass",
                Map.of("mechanism", "pii-redaction"),
                URI.create("regulus-audit://01J6X4ABCDEFG"));
    }
}
