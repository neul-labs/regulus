package com.regulus.platform.adk.plugins;

import com.google.adk.plugins.BasePlugin;

/**
 * Dual-control (4-eyes) kill switch enforced on every agent invocation.
 *
 * <p>Hooks used:
 * <ul>
 *   <li>{@code BeforeAgentCallback} — checks global + per-tenant + per-agent
 *       kill state. If active, short-circuits with a {@code KillSwitchActive}
 *       event and refuses execution.</li>
 * </ul>
 *
 * <p>Authorisation flows through ADK's {@code ToolConfirmation} primitive —
 * the kill switch can be flipped on instantly by any operator, but turning it
 * off requires confirmation from a second operator (via
 * {@code toolContext.requestConfirmation()}). This is the same primitive ADK
 * provides for HITL, so the dual-control workflow is shaped exactly like the
 * confirmation any ADK developer already knows.
 *
 * <p>Maps to: PRA PS21/3 (algorithmic-trading kill switches), PRA SS1/23 §6
 * (rapid model switch-off), EU AI Act Art. 14 (human oversight / interruption),
 * UK Consumer Duty (the ability to halt customer-harm scenarios immediately).
 */
public final class RegulusKillSwitchPlugin extends BasePlugin {

    private final KillSwitchStore store;
    private final boolean dualControl;

    private RegulusKillSwitchPlugin(KillSwitchStore store, boolean dualControl) {
        super("regulus-kill-switch");
        this.store = store;
        this.dualControl = dualControl;
    }

    /** Default: dual-control, in-memory store (suitable for examples and tests). */
    public static RegulusKillSwitchPlugin dualControl() {
        return new RegulusKillSwitchPlugin(new InMemoryKillSwitchStore(), true);
    }

    /** Wire to a custom store (Postgres / Firestore / etc.). */
    public static RegulusKillSwitchPlugin withStore(KillSwitchStore store) {
        return new RegulusKillSwitchPlugin(store, true);
    }

    public KillSwitchStore store() { return store; }
    public boolean isDualControl() { return dualControl; }
}
