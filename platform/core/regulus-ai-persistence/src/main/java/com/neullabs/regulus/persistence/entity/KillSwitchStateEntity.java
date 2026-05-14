package com.neullabs.regulus.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persistent kill switch state with optimistic locking for concurrent updates.
 */
@Entity
@Table(name = "kill_switch_states", indexes = {
    @Index(name = "idx_ks_scope", columnList = "scope"),
    @Index(name = "idx_ks_target_id", columnList = "targetId"),
    @Index(name = "idx_ks_active", columnList = "active")
})
public class KillSwitchStateEntity {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Scope scope;

    @Column(length = 100)
    private String targetId;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 500)
    private String reason;

    @Column(length = 100)
    private String activatedBy;

    @Column
    private Instant activatedAt;

    @Column(length = 100)
    private String deactivatedBy;

    @Column
    private Instant deactivatedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public enum Scope {
        GLOBAL,
        AGENT,
        MODEL,
        TOOL
    }

    protected KillSwitchStateEntity() {
    }

    public KillSwitchStateEntity(String id, Scope scope, String targetId) {
        this.id = id;
        this.scope = scope;
        this.targetId = targetId;
        this.active = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void activate(String reason, String activatedBy) {
        this.active = true;
        this.reason = reason;
        this.activatedBy = activatedBy;
        this.activatedAt = Instant.now();
        this.deactivatedBy = null;
        this.deactivatedAt = null;
        this.updatedAt = Instant.now();
    }

    public void deactivate(String deactivatedBy) {
        this.active = false;
        this.deactivatedBy = deactivatedBy;
        this.deactivatedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public Scope getScope() { return scope; }
    public String getTargetId() { return targetId; }
    public boolean isActive() { return active; }
    public String getReason() { return reason; }
    public String getActivatedBy() { return activatedBy; }
    public Instant getActivatedAt() { return activatedAt; }
    public String getDeactivatedBy() { return deactivatedBy; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    /**
     * Generate ID for a kill switch state.
     */
    public static String generateId(Scope scope, String targetId) {
        if (scope == Scope.GLOBAL) {
            return "global";
        }
        return scope.name().toLowerCase() + ":" + (targetId != null ? targetId : "default");
    }
}
