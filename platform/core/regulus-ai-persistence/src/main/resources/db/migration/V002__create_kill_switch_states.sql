-- Kill switch states table with optimistic locking
CREATE TABLE kill_switch_states (
    id VARCHAR(100) PRIMARY KEY,
    scope VARCHAR(20) NOT NULL,
    target_id VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    reason VARCHAR(500),
    activated_by VARCHAR(100),
    activated_at TIMESTAMP WITH TIME ZONE,
    deactivated_by VARCHAR(100),
    deactivated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX idx_ks_scope ON kill_switch_states(scope);
CREATE INDEX idx_ks_target_id ON kill_switch_states(target_id);
CREATE INDEX idx_ks_active ON kill_switch_states(active);
CREATE INDEX idx_ks_scope_active ON kill_switch_states(scope, active);

-- Insert global kill switch (always exists)
INSERT INTO kill_switch_states (id, scope, active, created_at, updated_at, version)
VALUES ('global', 'GLOBAL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Comment for documentation
COMMENT ON TABLE kill_switch_states IS 'Kill switch states for emergency shutdown. Supports PS21/3 operational resilience requirements.';
