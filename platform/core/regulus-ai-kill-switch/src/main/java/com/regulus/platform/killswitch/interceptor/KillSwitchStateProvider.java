package com.regulus.platform.killswitch.interceptor;

import com.regulus.platform.killswitch.model.KillSwitchState;

import java.util.Map;

/**
 * Interface for external kill switch state providers (Vault, ConfigHub, etc.)
 */
public interface KillSwitchStateProvider {

    /**
     * Load all kill switch states from the external provider.
     */
    Map<String, KillSwitchState> loadAllStates();

    /**
     * Save a kill switch state to the external provider.
     */
    void saveState(String key, KillSwitchState state);

    /**
     * Remove a kill switch state from the external provider.
     */
    void removeState(String key);

    /**
     * Check if the provider is available.
     */
    boolean isAvailable();
}
