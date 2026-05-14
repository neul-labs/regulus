package com.regulus.platform.governance.framework;

import com.regulus.platform.governance.FrameworkBinding;
import com.regulus.platform.governance.FrameworkControl;
import com.regulus.platform.governance.FrameworkKind;
import com.regulus.platform.governance.GovernanceFramework;

import java.util.Set;

/**
 * ISO/IEC 23894:2023 — Information technology — Artificial intelligence —
 * Guidance on risk management.
 *
 * <p>Companion standard to ISO/IEC 42001; provides AI-specific guidance on
 * how to apply ISO 31000 risk-management practices.
 */
public final class Iso23894Framework implements GovernanceFramework {

    @Override public String id()           { return "iso-23894"; }
    @Override public String displayName()  { return "ISO/IEC 23894 — AI risk management"; }
    @Override public String version()      { return "ISO/IEC 23894:2023"; }
    @Override public FrameworkKind kind()  { return FrameworkKind.STANDARD; }
    @Override public String authorityUrl() { return "https://www.iso.org/standard/77304.html"; }

    @Override public Set<FrameworkControl> controls() {
        return Set.of(
                new FrameworkControl("CL-5", "Process", "Risk-management process",
                        "Establish, implement, maintain, and continually improve a risk-management process for AI."),
                new FrameworkControl("CL-6.3", "Risk identification", "Identify AI-related risks",
                        "Identify risks specific to AI systems including data, model, and deployment risks."),
                new FrameworkControl("CL-6.4", "Risk analysis", "Analyse AI-related risks",
                        "Analyse identified risks to determine likelihood and consequence."),
                new FrameworkControl("CL-6.5", "Risk evaluation", "Evaluate against criteria",
                        "Compare analysed risks against risk criteria to inform treatment decisions."),
                new FrameworkControl("CL-6.6", "Risk treatment", "Treat AI risks",
                        "Select and implement treatment options proportionate to assessed risk."),
                new FrameworkControl("CL-6.7", "Monitoring and review", "Monitor risks",
                        "Continually monitor and review the risk environment.")
        );
    }

    @Override public Set<FrameworkBinding> bindings() {
        return Set.of(
                new FrameworkBinding("model-risk-tier", "CL-6.3",
                        "Tier classification is the risk-identification artefact for each model."),
                new FrameworkBinding("model-risk-tier", "CL-6.4",
                        "Tier mapping captures likelihood-and-consequence analysis."),
                new FrameworkBinding("dual-control-kill-switch", "CL-6.6",
                        "Kill switch is a risk-treatment option for the highest-likelihood / highest-consequence cases."),
                new FrameworkBinding("audit-trail", "CL-6.7",
                        "Audit trail + observability metrics are the monitor-and-review substrate.")
        );
    }
}
