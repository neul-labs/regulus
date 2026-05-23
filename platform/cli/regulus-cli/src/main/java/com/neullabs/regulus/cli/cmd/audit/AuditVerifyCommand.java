package com.neullabs.regulus.cli.cmd.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neullabs.regulus.observability.audit.integrity.HashChainAuditChain;
import com.neullabs.regulus.observability.audit.integrity.SealedAuditEvent;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * {@code regulus audit verify <chain.jsonl>}.
 *
 * <p>Reads a JSONL file where each line is a {@link SealedAuditEvent} (the
 * default logging sink format when audit integrity is enabled) and verifies
 * that the chain is intact: every event's {@code previousEventHash} matches
 * the predecessor's {@code eventHash}, every recomputed event hash matches,
 * and {@code chainIndex} is contiguous.
 *
 * <p>Exit codes: {@code 0} = chain verified; {@code 1} = chain tampered;
 * {@code 2} = file-system or parse error. Auditors run this against a copy
 * of the production audit log to confirm tamper-evidence offline.
 */
@Command(
        name = "verify",
        description = "Verify the integrity of a JSONL audit chain file.",
        mixinStandardHelpOptions = true)
public final class AuditVerifyCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to a JSONL audit chain file (one SealedAuditEvent per line).")
    Path chainFile;

    @Override
    public Integer call() {
        if (!Files.exists(chainFile)) {
            System.err.println("regulus audit verify: file not found: " + chainFile);
            return 2;
        }

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();

        List<SealedAuditEvent> events = new ArrayList<>();
        try (var lines = Files.lines(chainFile)) {
            int lineNumber = 0;
            for (String line : (Iterable<String>) lines::iterator) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    events.add(mapper.readValue(trimmed, SealedAuditEvent.class));
                } catch (Exception e) {
                    System.err.println("regulus audit verify: failed to parse line "
                            + lineNumber + ": " + e.getMessage());
                    return 2;
                }
            }
        } catch (Exception e) {
            System.err.println("regulus audit verify: I/O error: " + e.getMessage());
            return 2;
        }

        boolean ok = new HashChainAuditChain().verify(events);
        if (ok) {
            System.out.println("regulus audit verify: OK (" + events.size() + " events)");
            return 0;
        }
        System.out.println("regulus audit verify: FAILED — chain integrity broken");
        return 1;
    }
}
