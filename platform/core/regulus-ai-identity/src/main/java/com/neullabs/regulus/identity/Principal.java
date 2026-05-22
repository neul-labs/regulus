package com.neullabs.regulus.identity;

import java.util.Objects;

/**
 * Who is acting. {@code id} must uniquely identify the subject within the
 * tenant for the lifetime of an audit record; {@code displayName} is opaque
 * to policy and used only for human-readable audit output.
 */
public record Principal(String id, String displayName, PrincipalType type) {

    public Principal {
        Objects.requireNonNull(id, "Principal.id");
        Objects.requireNonNull(type, "Principal.type");
    }

    public enum PrincipalType {
        HUMAN,
        SERVICE,
        AGENT
    }
}
