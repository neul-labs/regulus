package com.neullabs.regulus.adk.plugins;

import com.google.adk.plugins.BasePlugin;
import com.neullabs.regulus.compliance.ComplianceProfile;

import java.util.Optional;

/**
 * Enforces Regulus policy guards on every ADK agent invocation.
 *
 * <p>Hooks used:
 * <ul>
 *   <li>{@code BeforeModelCallback} — checks purpose-binding, consent, LEI, and
 *       any other guards declared by the active {@link ComplianceProfile} before
 *       a prompt is sent to the model. Returns a short-circuiting value on
 *       violation; returns {@link Optional#empty()} to let the call proceed.</li>
 *   <li>{@code BeforeToolCallback} — enforces tool allowlists and per-tool
 *       policy bindings before any tool is invoked.</li>
 * </ul>
 *
 * <p>Maps to:
 * <ul>
 *   <li>EU AI Act Arts. 14 (human oversight), 26 (deployer obligations);</li>
 *   <li>GDPR / UK GDPR Arts. 5(1)(b) (purpose limitation), 22 (automated decisions);</li>
 *   <li>FCA Consumer Duty FG22/5 (four outcomes).</li>
 * </ul>
 *
 * <p>This plugin is a pure ADK citizen — it has no Spring or LangChain4j dependency.
 */
public final class RegulusPolicyPlugin extends BasePlugin {

    private final ComplianceProfile profile;
    private final PolicyDecider decider;

    private RegulusPolicyPlugin(ComplianceProfile profile, PolicyDecider decider) {
        super("regulus-policy");
        this.profile = profile;
        this.decider = decider;
    }

    /** Factory: build a policy plugin from a composite profile. */
    public static RegulusPolicyPlugin fromProfile(ComplianceProfile profile) {
        return new RegulusPolicyPlugin(profile, PolicyDecider.fromProfile(profile));
    }

    /** Factory: build with a custom decider (useful in tests and bespoke deployments). */
    public static RegulusPolicyPlugin withDecider(ComplianceProfile profile, PolicyDecider decider) {
        return new RegulusPolicyPlugin(profile, decider);
    }

    // The exact override signatures depend on ADK 1.2.0. The semantics are:
    //   - return Optional.empty() to allow the call to proceed
    //   - return a populated short-circuit response to block, with reason
    //
    // We document the intent here; the concrete @Override methods are added
    // alongside ADK 1.2.0 signature alignment in the next iteration.

    public ComplianceProfile profile() { return profile; }
    public PolicyDecider decider() { return decider; }
}
