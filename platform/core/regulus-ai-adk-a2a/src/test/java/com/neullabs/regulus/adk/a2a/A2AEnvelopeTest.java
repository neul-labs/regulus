package com.neullabs.regulus.adk.a2a;

import com.neullabs.regulus.adk.plugins.AuditSink;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2AEnvelopeTest {

    @Test
    void executorCarriesSinkAndSigningFlag() {
        AuditSink sink = AuditSink.stdout();
        RegulusAgentExecutor exec = new RegulusAgentExecutor(sink, true);
        assertThat(exec.auditSink()).isSameAs(sink);
        assertThat(exec.signRequests()).isTrue();
    }

    @Test
    void remoteAgentCarriesEndpointAndFlags() {
        URI endpoint = URI.create("https://partner.example/a2a");
        AuditSink sink = ev -> {}; // no-op
        RegulusRemoteA2AAgent remote = new RegulusRemoteA2AAgent(endpoint, sink, false);
        assertThat(remote.remoteEndpoint()).isEqualTo(endpoint);
        assertThat(remote.signRequests()).isFalse();
    }

    @Test
    void auditSinkLambdaCanBeUsed() {
        // Smoke: verify lambda-style AuditSink works in the envelope's accessor surface.
        AuditSink sink = (Map<String, Object> ev) -> { /* swallow */ };
        new RegulusAgentExecutor(sink, false);
        new RegulusRemoteA2AAgent(URI.create("https://x.example/a2a"), sink, false);
    }
}
