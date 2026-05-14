package com.regulus.platform.adk.plugins;

import com.google.adk.plugins.BasePlugin;
import com.regulus.platform.governance.FrameworkBinding;
import com.regulus.platform.governance.GovernanceFramework;
import com.regulus.platform.grc.EvidenceKind;
import com.regulus.platform.grc.GrcEvidenceAdapter;
import com.regulus.platform.grc.GrcEvidenceEnvelope;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fans audit events through configured {@link GrcEvidenceAdapter}s, mapping
 * the event's mechanism + clause to one or more framework control ids via
 * the active {@link GovernanceFramework}.
 *
 * <p>Registered downstream of {@link RegulusAuditPlugin} so the event is
 * fully populated. Errors in the adapter chain are caught and emitted as
 * their own audit events ({@code grc-adapter-failure}); the agent does
 * <strong>not</strong> fail because a downstream GRC tool is unavailable.
 *
 * <p>Maps to: NIST AI RMF GOVERN-1.5 (ongoing monitoring), ISO 42001
 * A.6.2.7 (event logs to interested parties), DORA Art. 12 (record-keeping
 * for ICT incidents). See ADR-011.
 */
public final class RegulusGovernanceEvidencePlugin extends BasePlugin {

    private final GovernanceFramework framework;
    private final List<GrcEvidenceAdapter> adapters;
    private final AuditSink auditSink;
    /** mechanism -> set of (controlFrameworkId, controlId) bindings, pre-indexed. */
    private final Map<String, Set<ControlRef>> bindingsByMechanism = new ConcurrentHashMap<>();

    private RegulusGovernanceEvidencePlugin(GovernanceFramework framework,
                                            List<GrcEvidenceAdapter> adapters,
                                            AuditSink auditSink) {
        super("regulus-governance-evidence");
        this.framework = framework;
        this.adapters = List.copyOf(adapters);
        this.auditSink = auditSink;
        indexBindings();
    }

    public static RegulusGovernanceEvidencePlugin forFramework(
            GovernanceFramework framework, GrcEvidenceAdapter... adapters) {
        return new RegulusGovernanceEvidencePlugin(framework, List.of(adapters), AuditSink.stdout());
    }

    public static RegulusGovernanceEvidencePlugin forFramework(
            GovernanceFramework framework, List<GrcEvidenceAdapter> adapters, AuditSink auditSink) {
        return new RegulusGovernanceEvidencePlugin(framework, adapters, auditSink);
    }

    private void indexBindings() {
        // Pull from CompositeGovernanceFramework correctly: framework.bindings()
        // already returns the union across components.
        for (FrameworkBinding b : framework.bindings()) {
            bindingsByMechanism
                    .computeIfAbsent(b.mechanism(), k -> ConcurrentHashMap.newKeySet())
                    .add(new ControlRef(framework.id(), b.controlId()));
        }
    }

    /**
     * Hand the plugin a fully-populated audit event payload. The plugin
     * derives one or more {@link GrcEvidenceEnvelope}s — one per matching
     * framework control — and dispatches to every adapter.
     */
    public void onAuditEvent(Map<String, Object> auditEvent) {
        String mechanism = (String) auditEvent.get("mechanism");
        if (mechanism == null) return;
        Set<ControlRef> refs = bindingsByMechanism.getOrDefault(mechanism, Set.of());
        if (refs.isEmpty()) return;

        for (ControlRef ref : refs) {
            GrcEvidenceEnvelope envelope = toEnvelope(auditEvent, ref);
            for (GrcEvidenceAdapter adapter : adapters) {
                try {
                    adapter.emit(envelope);
                } catch (Exception e) {
                    auditSink.emit(Map.of(
                            "event_id", UUID.randomUUID().toString(),
                            "occurred_at", Instant.now().toString(),
                            "action", "grc-adapter-failure",
                            "vendor", adapter.vendorId(),
                            "control_framework_id", ref.frameworkId(),
                            "framework_control_id", ref.controlId(),
                            "error", e.getMessage()
                    ));
                }
            }
        }
    }

    private GrcEvidenceEnvelope toEnvelope(Map<String, Object> auditEvent, ControlRef ref) {
        String eventId  = (String) auditEvent.getOrDefault("event_id", UUID.randomUUID().toString());
        Instant when   = auditEvent.containsKey("occurred_at")
                ? Instant.parse((String) auditEvent.get("occurred_at"))
                : Instant.now();
        EvidenceKind kind = switch ((String) auditEvent.getOrDefault("action", "model-call")) {
            case "policy.block", "policy.require-confirmation"        -> EvidenceKind.POLICY_ENFORCEMENT;
            case "kill-switch-activate", "kill-switch-deactivated"    -> EvidenceKind.INCIDENT;
            case "exception-recorded"                                  -> EvidenceKind.EXCEPTION;
            default                                                    -> EvidenceKind.CONTROL_TEST;
        };
        return new GrcEvidenceEnvelope(
                eventId,
                when,
                ref.frameworkId(),
                ref.controlId(),
                (String) auditEvent.get("compliance_profile_id"),
                (String) auditEvent.get("clause_citation"),
                kind,
                (String) auditEvent.getOrDefault("actor", "unknown"),
                (String) auditEvent.getOrDefault("result", "pass"),
                auditEvent,
                auditEventLink(eventId)
        );
    }

    private URI auditEventLink(String eventId) {
        try {
            return URI.create("regulus-audit://" + eventId);
        } catch (Exception e) {
            return null;
        }
    }

    public GovernanceFramework framework() { return framework; }
    public List<GrcEvidenceAdapter> adapters() { return adapters; }

    private record ControlRef(String frameworkId, String controlId) {}
}
