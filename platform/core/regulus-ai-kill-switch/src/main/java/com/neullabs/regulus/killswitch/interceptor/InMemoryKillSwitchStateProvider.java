package com.neullabs.regulus.killswitch.interceptor;

import com.neullabs.regulus.killswitch.model.KillSwitchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of KillSwitchStateProvider for development and testing.
 * Production environments should use Vault or ConfigHub implementations.
 */
public class InMemoryKillSwitchStateProvider implements KillSwitchStateProvider {

    private static final Logger log = LoggerFactory.getLogger(InMemoryKillSwitchStateProvider.class);

    private final Map<String, KillSwitchState> states = new ConcurrentHashMap<>();

    @Override
    public Map<String, KillSwitchState> loadAllStates() {
        log.debug("Loading {} kill switch states from memory", states.size());
        return Map.copyOf(states);
    }

    @Override
    public void saveState(String key, KillSwitchState state) {
        states.put(key, state);
        log.debug("Saved kill switch state for key '{}'", key);
    }

    @Override
    public void removeState(String key) {
        states.remove(key);
        log.debug("Removed kill switch state for key '{}'", key);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
