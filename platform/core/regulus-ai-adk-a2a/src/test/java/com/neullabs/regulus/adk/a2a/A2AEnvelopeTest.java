package com.neullabs.regulus.adk.a2a;

import com.neullabs.regulus.adk.a2a.signing.A2ARequestSigner;
import com.neullabs.regulus.adk.a2a.signing.A2AEnvelope;
import com.neullabs.regulus.adk.a2a.signing.SignatureException;
import com.neullabs.regulus.adk.a2a.signing.SignedEnvelope;
import com.neullabs.regulus.adk.a2a.signing.VerifiedCaller;
import com.neullabs.regulus.adk.plugins.AuditSink;
import com.neullabs.regulus.identity.Identity;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2AEnvelopeTest {

    private static final A2ARequestSigner STUB_SIGNER = new A2ARequestSigner() {
        @Override public SignedEnvelope sign(A2AEnvelope envelope, Identity caller) {
            throw new UnsupportedOperationException("stub");
        }
        @Override public VerifiedCaller verify(SignedEnvelope envelope) throws SignatureException {
            throw new SignatureException("stub");
        }
    };

    @Test
    void executorCarriesSinkAndSignerPresence() {
        AuditSink sink = AuditSink.stdout();
        RegulusAgentExecutor exec = new RegulusAgentExecutor(sink, STUB_SIGNER);
        assertThat(exec.auditSink()).isSameAs(sink);
        assertThat(exec.signRequests()).isTrue();
        assertThat(exec.signer()).isPresent();
    }

    @Test
    void remoteAgentCarriesEndpointAndAbsentSigner() {
        URI endpoint = URI.create("https://partner.example/a2a");
        AuditSink sink = ev -> {}; // no-op
        RegulusRemoteA2AAgent remote = new RegulusRemoteA2AAgent(endpoint, sink);
        assertThat(remote.remoteEndpoint()).isEqualTo(endpoint);
        assertThat(remote.signRequests()).isFalse();
        assertThat(remote.signer()).isEmpty();
    }

    @Test
    void auditSinkLambdaCanBeUsed() {
        AuditSink sink = (Map<String, Object> ev) -> { /* swallow */ };
        new RegulusAgentExecutor(sink);
        new RegulusRemoteA2AAgent(URI.create("https://x.example/a2a"), sink);
    }
}
