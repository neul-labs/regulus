package com.neullabs.regulus.grc.adapter;

import com.neullabs.regulus.grc.EvidenceKind;
import com.neullabs.regulus.grc.GrcEvidenceEnvelope;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StdoutAdapterTest {

    @Test
    void vendorIdIsStdout() {
        assertThat(new StdoutAdapter().vendorId()).isEqualTo("stdout");
    }

    @Test
    void emitDoesNotThrow() {
        StdoutAdapter adapter = new StdoutAdapter();
        adapter.emit(new GrcEvidenceEnvelope(
                "01J6X4ABCDEFG", Instant.now(),
                "iso-42001", "A.7.3",
                "uk-gdpr", "Art. 25",
                EvidenceKind.CONTROL_TEST,
                "user:1", "pass",
                Map.of("mechanism", "pii-redaction"),
                URI.create("regulus-audit://01J6X4ABCDEFG")));
    }

    @Test
    void healthCheckIsBenign() {
        new StdoutAdapter().healthCheck();
    }
}
