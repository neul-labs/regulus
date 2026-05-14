package com.neullabs.regulus.governance.framework;

import com.neullabs.regulus.governance.FrameworkBinding;
import com.neullabs.regulus.governance.FrameworkControl;
import com.neullabs.regulus.governance.FrameworkKind;
import com.neullabs.regulus.governance.GovernanceFramework;

import java.util.Set;

/**
 * ISO/IEC 23053:2022 — Framework for Artificial Intelligence (AI) systems
 * using Machine Learning (ML).
 *
 * <p>Defines a reference architecture and life-cycle for AI/ML systems.
 * Regulus binds to the operational components Regulus surfaces.
 */
public final class Iso23053Framework implements GovernanceFramework {

    @Override public String id()           { return "iso-23053"; }
    @Override public String displayName()  { return "ISO/IEC 23053 — AI/ML system framework"; }
    @Override public String version()      { return "ISO/IEC 23053:2022"; }
    @Override public FrameworkKind kind()  { return FrameworkKind.STANDARD; }
    @Override public String authorityUrl() { return "https://www.iso.org/standard/74438.html"; }

    @Override public Set<FrameworkControl> controls() {
        return Set.of(
                new FrameworkControl("CL-6.2", "Reference architecture", "Components and roles",
                        "Identify reference architecture components and the roles of stakeholders."),
                new FrameworkControl("CL-6.5", "Reference architecture", "Operations",
                        "Operate the AI/ML system: deployment, monitoring, governance."),
                new FrameworkControl("CL-7", "Lifecycle", "AI/ML life cycle",
                        "Define and follow the life cycle including verification and validation."),
                new FrameworkControl("CL-8", "Functional view", "Functional decomposition",
                        "Decompose the AI/ML system into functional components.")
        );
    }

    @Override public Set<FrameworkBinding> bindings() {
        return Set.of(
                new FrameworkBinding("model-inventory", "CL-6.2",
                        "ModelRegistry surfaces the components of the reference architecture."),
                new FrameworkBinding("audit-trail", "CL-6.5",
                        "Audit + observability are the operations layer."),
                new FrameworkBinding("model-risk-tier", "CL-7",
                        "Tier classification feeds the verification and validation gate.")
        );
    }
}
