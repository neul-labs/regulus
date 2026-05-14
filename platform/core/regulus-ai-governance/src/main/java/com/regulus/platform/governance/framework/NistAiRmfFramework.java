package com.regulus.platform.governance.framework;

import com.regulus.platform.governance.FrameworkBinding;
import com.regulus.platform.governance.FrameworkControl;
import com.regulus.platform.governance.FrameworkKind;
import com.regulus.platform.governance.GovernanceFramework;

import java.util.Set;

/**
 * NIST AI Risk Management Framework 1.0 (NIST AI 100-1, January 2023).
 *
 * <p>Core organised around four functions: GOVERN, MAP, MEASURE, MANAGE.
 * Each function has categories and subcategories. Regulus binds its
 * mechanisms to selected subcategories that align directly with the
 * controls Regulus implements; the full subcategory set is not duplicated
 * here — the {@link #authorityUrl()} points at the NIST publication.
 */
public final class NistAiRmfFramework implements GovernanceFramework {

    @Override public String id()           { return "nist-ai-rmf"; }
    @Override public String displayName()  { return "NIST AI Risk Management Framework"; }
    @Override public String version()      { return "1.0 (2023)"; }
    @Override public FrameworkKind kind()  { return FrameworkKind.VOLUNTARY; }
    @Override public String authorityUrl() { return "https://www.nist.gov/itl/ai-risk-management-framework"; }

    @Override public Set<FrameworkControl> controls() {
        return Set.of(
                // GOVERN
                new FrameworkControl("GOVERN-1.1", "GOVERN", "Risk-management policies and procedures",
                        "Policies, processes, procedures, and practices across the organization related to mapping, measuring, and managing AI risks are documented and transparent."),
                new FrameworkControl("GOVERN-1.5", "GOVERN", "Ongoing monitoring and periodic review",
                        "Ongoing monitoring and periodic review of the risk-management process and its outcomes."),
                new FrameworkControl("GOVERN-2.1", "GOVERN", "Roles and responsibilities",
                        "Roles, responsibilities, and accountability for designing, developing, deploying, evaluating and acquiring AI systems are documented."),
                new FrameworkControl("GOVERN-4.1", "GOVERN", "Culture of risk management",
                        "Organizational policies and practices promote a critical thinking and safety-first mindset in the design, development, deployment, and use of AI systems."),
                new FrameworkControl("GOVERN-6.1", "GOVERN", "Third-party risk",
                        "Policies and procedures are in place to address AI risks and benefits arising from third-party software and data."),

                // MAP
                new FrameworkControl("MAP-1.1", "MAP", "Context characterization",
                        "Intended purposes, potentially beneficial uses, context-specific laws, norms and expectations, and prospective settings are understood and documented."),
                new FrameworkControl("MAP-2.3", "MAP", "Capabilities and uses",
                        "AI system capabilities, targeted usage, goals and expected benefits are understood relative to context."),
                new FrameworkControl("MAP-4.1", "MAP", "Risk classification",
                        "Approaches for mapping AI technology and legal risks of its components — including the use of third-party data or software — are in place."),

                // MEASURE
                new FrameworkControl("MEASURE-1.1", "MEASURE", "Trustworthy characteristics identified",
                        "Approaches and metrics for measurement of AI risks are selected for implementation."),
                new FrameworkControl("MEASURE-2.7", "MEASURE", "Security and resiliency",
                        "AI system security and resilience — as identified in the MAP function — are evaluated and documented."),
                new FrameworkControl("MEASURE-2.8", "MEASURE", "Information integrity",
                        "Risks associated with transparency and accountability — as identified in MAP — are examined and documented."),
                new FrameworkControl("MEASURE-3.2", "MEASURE", "Risk-tracking metrics",
                        "Risk-tracking approaches are considered for settings where AI risks are difficult to assess using currently available measurement techniques."),

                // MANAGE
                new FrameworkControl("MANAGE-1.3", "MANAGE", "Risk treatment",
                        "Responses to the AI risks deemed high priority — as identified by MAP — are developed, planned and documented."),
                new FrameworkControl("MANAGE-2.2", "MANAGE", "Incident response",
                        "Mechanisms are in place and applied to sustain the value of deployed AI systems."),
                new FrameworkControl("MANAGE-4.1", "MANAGE", "Decommissioning",
                        "Post-deployment AI system monitoring plans are implemented, including mechanisms for capturing and evaluating input from users.")
        );
    }

    @Override public Set<FrameworkBinding> bindings() {
        return Set.of(
                new FrameworkBinding("policy-engine", "GOVERN-1.1",
                        "RegulusPolicyPlugin documents policies in code as ComplianceProfile + ControlBinding sets, surfaced in the coverage matrix."),
                new FrameworkBinding("audit-trail", "GOVERN-1.5",
                        "RegulusAuditPlugin event stream is the substrate for ongoing monitoring and periodic review."),
                new FrameworkBinding("senior-management-arrangements", "GOVERN-2.1",
                        "smf_holder attribution per audit event keeps roles and responsibilities traceable per action."),
                new FrameworkBinding("dual-control-kill-switch", "GOVERN-4.1",
                        "Asymmetric activation/deactivation models a 'safety-first' default for high-stakes interventions."),
                new FrameworkBinding("third-party-risk", "GOVERN-6.1",
                        "RegulusModelRiskPlugin + model registry track the LLM provider as a third party with documented tiering."),
                new FrameworkBinding("purpose-binding", "MAP-1.1",
                        "Every invocation carries an explicit purpose_code recorded in audit and policy contexts."),
                new FrameworkBinding("model-risk-tier", "MAP-4.1",
                        "RegulusModelRiskPlugin.Tier represents the per-model classification produced by the MAP function."),
                new FrameworkBinding("audit-trail", "MEASURE-1.1",
                        "Structured audit events are the per-invocation metric stream for measurement."),
                new FrameworkBinding("data-residency", "MEASURE-2.7",
                        "RegulusDataResidencyPlugin fail-closed at startup is a measurable security control."),
                new FrameworkBinding("transparency-disclosure", "MEASURE-2.8",
                        "Provenance fields (model_id, model_version) on every audit event support transparency measurement."),
                new FrameworkBinding("incident-classification", "MANAGE-2.2",
                        "Audit event severity tagging and ICT incident schema feed the incident-response loop."),
                new FrameworkBinding("post-market-monitoring", "MANAGE-4.1",
                        "RegulusAuditPlugin + observability emit continuous monitoring signals.")
        );
    }
}
