package com.neullabs.regulus.identity;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What regulators need to know about a {@link Principal} to evaluate a request:
 * which tenant they belong to, which jurisdiction governs the call, which
 * purpose codes and lawful bases they are authorised under, and which roles
 * they hold. {@code extensions} carries adapter-specific custom claims as
 * strings so the trust boundary stays serialisation-friendly (callers
 * encoding structured values must JSON-stringify them).
 */
public record Claims(
        String tenantId,
        Jurisdiction jurisdiction,
        Set<String> purposeCodes,
        Set<String> roles,
        Set<String> lawfulBases,
        Map<String, String> extensions) {

    public Claims {
        Objects.requireNonNull(tenantId, "Claims.tenantId");
        Objects.requireNonNull(jurisdiction, "Claims.jurisdiction");
        purposeCodes = purposeCodes == null ? Set.of() : Set.copyOf(purposeCodes);
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        lawfulBases = lawfulBases == null ? Set.of() : Set.copyOf(lawfulBases);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasPurpose(String purposeCode) {
        return purposeCodes.contains(purposeCode);
    }
}
