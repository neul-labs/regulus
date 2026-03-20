package com.regulus.platform.adk.plugins;

import com.google.adk.plugins.BasePlugin;
import com.regulus.platform.compliance.AuditSchema;
import com.regulus.platform.compliance.ComplianceProfile;

/**
 * Emits an immutable audit record for every consequential agent action and
 * supplies a regulation-aware {@code EventCompactor} for ADK's context-engineering
 * subsystem.
 *
 * <p>Hooks used:
 * <ul>
 *   <li>{@code AfterAgentCallback}, {@code AfterModelCallback}, {@code AfterToolCallback}
 *       — emits a structured event per terminal action.</li>
 *   <li>Custom {@code EventCompactor} (registered on the {@code App}) — picks the
 *       retention window from the active {@link ComplianceProfile} and
 *       summarises older events via a {@code BaseEventSummarizer} so the
 *       context window stays bounded without losing audit fidelity.</li>
 * </ul>
 *
 * <p>Audit sinks: Kafka topic ({@link Builder#toKafka(String)}) or a custom
 * {@link AuditSink} implementation. Events conform to the
 * {@link AuditSchema} of the active profile; missing required fields cause a
 * fail-closed startup error.
 *
 * <p>Maps to: EU AI Act Arts. 12 (logging) and 19 (log retention);
 * GDPR Art. 30 (records of processing); FCA SYSC 9 (record-keeping);
 * DORA Art. 12 (ICT incident records); UK NHS DSPT 6.x (incident management).
 */
public final class RegulusAuditPlugin extends BasePlugin {

    private final ComplianceProfile profile;
    private final AuditSink sink;

    private RegulusAuditPlugin(ComplianceProfile profile, AuditSink sink) {
        super("regulus-audit");
        this.profile = profile;
        this.sink = sink;
    }

    public static Builder forProfile(ComplianceProfile profile) {
        return new Builder(profile);
    }

    public ComplianceProfile profile() { return profile; }
    public AuditSink sink() { return sink; }

    public static final class Builder {
        private final ComplianceProfile profile;
        private AuditSink sink;

        Builder(ComplianceProfile profile) { this.profile = profile; }

        public Builder toKafka(String topic) {
            this.sink = AuditSink.kafka(topic);
            return this;
        }

        public Builder toSink(AuditSink sink) {
            this.sink = sink;
            return this;
        }

        public RegulusAuditPlugin build() {
            if (sink == null) sink = AuditSink.stdout();
            return new RegulusAuditPlugin(profile, sink);
        }
    }
}
