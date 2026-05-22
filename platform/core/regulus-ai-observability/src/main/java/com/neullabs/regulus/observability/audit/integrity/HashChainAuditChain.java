package com.neullabs.regulus.observability.audit.integrity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neullabs.regulus.identity.crypto.KeyProvider;
import com.neullabs.regulus.observability.audit.AuditEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default {@link AuditChain}. SHA-256 over a canonical JSON representation
 * of each event; optional detached signature when a {@link KeyProvider} is
 * supplied. Canonicalisation uses Jackson with keys sorted alphabetically
 * so two semantically equal events produce identical hashes regardless of
 * map ordering.
 *
 * <p>Signature implementation is left as a follow-up — the SPI shape is
 * stable and {@code keyId()} is recorded in every sealed event so
 * verifiers know which key to fetch even before the signing milestone
 * lands.
 */
public final class HashChainAuditChain implements AuditChain {

    public static final String GENESIS_HASH = "0".repeat(64);

    private final ObjectMapper canonicalMapper;
    private final Optional<KeyProvider> keyProvider;
    private final String keyId;
    private final AtomicReference<String> previousHash = new AtomicReference<>(GENESIS_HASH);
    private final AtomicLong nextIndex = new AtomicLong(0L);

    public HashChainAuditChain() {
        this(null);
    }

    public HashChainAuditChain(KeyProvider keyProvider) {
        this.canonicalMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();
        this.keyProvider = Optional.ofNullable(keyProvider);
        this.keyId = keyProvider == null ? "" : keyProvider.defaultSigningKeyId();
    }

    @Override
    public synchronized SealedAuditEvent append(AuditEvent event) {
        String prev = previousHash.get();
        String hash = hash(prev, event);
        long index = nextIndex.getAndIncrement();
        previousHash.set(hash);
        return new SealedAuditEvent(event, index, prev, hash, Optional.empty(), keyId);
    }

    @Override
    public boolean verify(List<SealedAuditEvent> chain) {
        if (chain == null || chain.isEmpty()) {
            return true;
        }
        String prev = GENESIS_HASH;
        long expectedIndex = 0L;
        for (SealedAuditEvent sealed : chain) {
            if (sealed.chainIndex() != expectedIndex) {
                return false;
            }
            if (!sealed.previousEventHash().equals(prev)) {
                return false;
            }
            String recomputed = hash(prev, sealed.event());
            if (!recomputed.equals(sealed.eventHash())) {
                return false;
            }
            prev = sealed.eventHash();
            expectedIndex++;
        }
        return true;
    }

    private String hash(String previousHash, AuditEvent event) {
        try {
            byte[] canonical = canonicalMapper.writeValueAsBytes(event);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(previousHash.getBytes(StandardCharsets.US_ASCII));
            md.update((byte) '|');
            md.update(canonical);
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to canonicalise audit event", e);
        }
    }
}
