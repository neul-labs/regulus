-- Audit events table for compliance and regulatory reporting
CREATE TABLE audit_events (
    id VARCHAR(36) PRIMARY KEY,
    correlation_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id VARCHAR(100),
    agent_id VARCHAR(100),
    model_id VARCHAR(100),
    tool_name VARCHAR(100),
    input_summary TEXT,
    output_summary TEXT,
    outcome VARCHAR(20),
    duration_ms BIGINT,
    token_count INTEGER,
    estimated_cost DECIMAL(10, 6),
    policy_decision VARCHAR(100),
    policy_violations TEXT,
    lei VARCHAR(100),
    purpose_code VARCHAR(50),
    consent_granted BOOLEAN,
    kill_switch_active BOOLEAN,
    metadata_json TEXT,
    source_ip VARCHAR(50),
    user_agent VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_audit_correlation_id ON audit_events(correlation_id);
CREATE INDEX idx_audit_timestamp ON audit_events(timestamp);
CREATE INDEX idx_audit_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_user_id ON audit_events(user_id);
CREATE INDEX idx_audit_agent_id ON audit_events(agent_id);
CREATE INDEX idx_audit_lei ON audit_events(lei);
CREATE INDEX idx_audit_outcome ON audit_events(outcome);

-- Composite index for time-range queries with filters
CREATE INDEX idx_audit_time_type ON audit_events(timestamp, event_type);
CREATE INDEX idx_audit_time_user ON audit_events(timestamp, user_id);

-- Comment for documentation
COMMENT ON TABLE audit_events IS 'Immutable audit log for AI agent invocations. Supports FCA SS1/23 and PS21/3 compliance.';
