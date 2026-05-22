package com.neullabs.regulus.cli.cmd.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neullabs.regulus.observability.audit.AuditEvent;
import com.neullabs.regulus.observability.audit.integrity.HashChainAuditChain;
import com.neullabs.regulus.observability.audit.integrity.SealedAuditEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AuditVerifyCommandTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .build();

    private AuditEvent event(String suffix) {
        return AuditEvent.builder()
                .type(AuditEvent.EventType.LLM_CALL)
                .timestamp(Instant.parse("2026-05-22T10:00:00Z"))
                .correlationId("corr-" + suffix)
                .userId("u-1")
                .operation("llm.call")
                .resource("gemini-1.5")
                .outcome(AuditEvent.Outcome.SUCCESS)
                .message(suffix)
                .build();
    }

    private Path writeChain(Path dir, List<SealedAuditEvent> events) throws Exception {
        Path file = dir.resolve("chain.jsonl");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file))) {
            for (SealedAuditEvent ev : events) {
                w.println(mapper.writeValueAsString(ev));
            }
        }
        return file;
    }

    @Test
    void intactChainReturnsZero(@TempDir Path tmp) throws Exception {
        HashChainAuditChain chain = new HashChainAuditChain();
        List<SealedAuditEvent> events = List.of(chain.append(event("a")), chain.append(event("b")));
        Path file = writeChain(tmp, events);

        int code = new CommandLine(new AuditVerifyCommand()).execute(file.toString());
        assertThat(code).isEqualTo(0);
    }

    @Test
    void tamperedChainReturnsOne(@TempDir Path tmp) throws Exception {
        HashChainAuditChain chain = new HashChainAuditChain();
        SealedAuditEvent ev1 = chain.append(event("a"));
        SealedAuditEvent ev2 = chain.append(event("b"));
        // Forge a wrong eventHash on the second one
        SealedAuditEvent forged = new SealedAuditEvent(
                ev2.event(), ev2.chainIndex(), ev2.previousEventHash(),
                "0".repeat(64), Optional.empty(), ev2.keyId());
        Path file = writeChain(tmp, List.of(ev1, forged));

        int code = new CommandLine(new AuditVerifyCommand()).execute(file.toString());
        assertThat(code).isEqualTo(1);
    }

    @Test
    void missingFileReturnsTwo(@TempDir Path tmp) {
        int code = new CommandLine(new AuditVerifyCommand())
                .execute(tmp.resolve("nope.jsonl").toString());
        assertThat(code).isEqualTo(2);
    }
}
