package com.neullabs.regulus.governance.framework;

import com.neullabs.regulus.governance.FrameworkBinding;
import com.neullabs.regulus.governance.FrameworkControl;
import com.neullabs.regulus.governance.FrameworkKind;
import com.neullabs.regulus.governance.GovernanceFramework;

import java.util.Set;

/**
 * ISO/IEC 42001:2023 — Artificial Intelligence Management System (AIMS).
 *
 * <p>Same shape as ISO/IEC 27001 but for AI. Includes nine Annex A control
 * objectives (A.2 — A.10) and roughly 38 controls. Certification requires
 * a documented Statement of Applicability — Regulus generates this from
 * {@link com.neullabs.regulus.governance.GovernanceProgramState}.
 *
 * <p>EN ISO/IEC 42001:2026 confirmed European adoption. Annex A control
 * inventory below covers the canonical objectives; the full enumeration
 * sits in the ISO publication referenced via {@link #authorityUrl()}.
 */
public final class Iso42001Framework implements GovernanceFramework {

    @Override public String id()           { return "iso-42001"; }
    @Override public String displayName()  { return "ISO/IEC 42001 — AI Management System"; }
    @Override public String version()      { return "ISO/IEC 42001:2023 (EN adoption 2026)"; }
    @Override public FrameworkKind kind()  { return FrameworkKind.CERTIFIABLE; }
    @Override public String authorityUrl() { return "https://www.iso.org/standard/42001"; }

    @Override public Set<FrameworkControl> controls() {
        return Set.of(
                // A.2 — Policies related to AI
                new FrameworkControl("A.2.2", "A.2 Policies", "AI policy",
                        "An AI policy is documented, approved, communicated, and reviewed."),
                new FrameworkControl("A.2.3", "A.2 Policies", "Alignment with other policies",
                        "AI policy aligns with related organizational policies (security, privacy, ethics)."),
                new FrameworkControl("A.2.4", "A.2 Policies", "Review of AI policy",
                        "The AI policy is reviewed at planned intervals and when significant changes occur."),

                // A.3 — Internal organization
                new FrameworkControl("A.3.2", "A.3 Internal organization", "AI roles and responsibilities",
                        "Roles and responsibilities for AI systems are defined, documented, and communicated."),
                new FrameworkControl("A.3.3", "A.3 Internal organization", "Reporting of concerns",
                        "A process exists for personnel to report AI-related concerns confidentially."),

                // A.4 — Resources for AI systems
                new FrameworkControl("A.4.2", "A.4 Resources", "Resource documentation",
                        "Resources allocated to AI systems are identified and documented (data, tooling, compute, expertise)."),
                new FrameworkControl("A.4.3", "A.4 Resources", "Data resources",
                        "Data used in the AI system is documented including provenance and quality."),
                new FrameworkControl("A.4.4", "A.4 Resources", "Tooling resources",
                        "Tools used in AI system development and operation are documented."),
                new FrameworkControl("A.4.5", "A.4 Resources", "System and computing resources",
                        "Computing and system resources used by AI systems are documented."),
                new FrameworkControl("A.4.6", "A.4 Resources", "Human resources",
                        "Competence requirements for AI roles are defined and met."),

                // A.5 — Assessing impacts of AI systems
                new FrameworkControl("A.5.2", "A.5 Impact assessment", "AI system impact assessment",
                        "Impact of AI systems on individuals, groups, and society is assessed."),
                new FrameworkControl("A.5.3", "A.5 Impact assessment", "Documentation of impact",
                        "Impact-assessment results are documented and used."),
                new FrameworkControl("A.5.4", "A.5 Impact assessment", "Assessment timing",
                        "Impact assessment is performed before deployment and after material change."),
                new FrameworkControl("A.5.5", "A.5 Impact assessment", "Impact on individuals",
                        "Specific assessment of impact on individuals' rights and freedoms."),

                // A.6 — AI system life cycle
                new FrameworkControl("A.6.1.2", "A.6 AI system lifecycle", "Objectives for responsible AI",
                        "Objectives for the responsible development of AI systems are defined."),
                new FrameworkControl("A.6.1.3", "A.6 AI system lifecycle", "Documentation of processes",
                        "Processes for responsible AI development are documented."),
                new FrameworkControl("A.6.2.2", "A.6 AI system lifecycle", "AI system requirements",
                        "Requirements for AI systems, including risk treatment, are specified."),
                new FrameworkControl("A.6.2.3", "A.6 AI system lifecycle", "Verification and validation",
                        "AI systems are verified and validated against requirements."),
                new FrameworkControl("A.6.2.4", "A.6 AI system lifecycle", "Deployment",
                        "Criteria and process for deployment of AI systems are established."),
                new FrameworkControl("A.6.2.5", "A.6 AI system lifecycle", "Operation and monitoring",
                        "AI systems in operation are monitored against requirements."),
                new FrameworkControl("A.6.2.6", "A.6 AI system lifecycle", "Technical documentation",
                        "Technical documentation for AI systems is produced and maintained."),
                new FrameworkControl("A.6.2.7", "A.6 AI system lifecycle", "Recording of event logs",
                        "Logs of AI system events are produced and retained."),
                new FrameworkControl("A.6.2.8", "A.6 AI system lifecycle", "Change management",
                        "Changes to AI systems are controlled and documented."),

                // A.7 — Data for AI systems
                new FrameworkControl("A.7.2", "A.7 Data", "Data for development",
                        "Processes for managing data used in AI development are defined."),
                new FrameworkControl("A.7.3", "A.7 Data", "Data quality",
                        "Data quality is assessed and documented."),
                new FrameworkControl("A.7.4", "A.7 Data", "Data provenance",
                        "Data provenance is recorded and verifiable."),
                new FrameworkControl("A.7.5", "A.7 Data", "Data preparation",
                        "Data preparation processes are documented."),

                // A.8 — Information for interested parties
                new FrameworkControl("A.8.2", "A.8 Information", "System information",
                        "System information for users and other parties is provided."),
                new FrameworkControl("A.8.3", "A.8 Information", "External reporting",
                        "Mechanism for reporting external concerns about the AI system is in place."),
                new FrameworkControl("A.8.4", "A.8 Information", "Communication of incidents",
                        "AI-related incidents are communicated to relevant interested parties."),
                new FrameworkControl("A.8.5", "A.8 Information", "Information for users",
                        "Information enabling users to use the system appropriately is provided."),

                // A.9 — Use of AI systems
                new FrameworkControl("A.9.2", "A.9 Use of AI", "Intended use",
                        "AI systems are used in alignment with their documented intended purpose."),
                new FrameworkControl("A.9.3", "A.9 Use of AI", "Objectives for responsible use",
                        "Responsible-use objectives are defined and communicated."),
                new FrameworkControl("A.9.4", "A.9 Use of AI", "Intended-use boundaries",
                        "Boundaries of intended use are documented; out-of-scope use is prevented."),

                // A.10 — Third-party and customer relationships
                new FrameworkControl("A.10.2", "A.10 Third-party", "Third-party allocation of responsibilities",
                        "Responsibilities between organization and third parties are allocated and documented."),
                new FrameworkControl("A.10.3", "A.10 Third-party", "Third-party suppliers",
                        "AI-related third-party suppliers are managed throughout their lifecycle."),
                new FrameworkControl("A.10.4", "A.10 Third-party", "Customer responsibilities",
                        "Customer responsibilities relating to the AI system are defined and communicated.")
        );
    }

    @Override public Set<FrameworkBinding> bindings() {
        return Set.of(
                new FrameworkBinding("policy-engine", "A.2.2",
                        "ComplianceProfile + GovernanceFramework declarations are the firm's documented AI policy in code."),
                new FrameworkBinding("senior-management-arrangements", "A.3.2",
                        "smf_holder attribution maps to documented AI roles and responsibilities."),
                new FrameworkBinding("model-inventory", "A.4.4",
                        "ModelRegistry is the canonical record of tooling resources (models, executors)."),
                new FrameworkBinding("model-risk-tier", "A.5.2",
                        "Per-model risk tier feeds the AI system impact assessment."),
                new FrameworkBinding("audit-trail", "A.6.2.7",
                        "RegulusAuditPlugin produces the AIMS-required event log."),
                new FrameworkBinding("post-market-monitoring", "A.6.2.5",
                        "Operation and monitoring outputs feed back into the AIMS review cycle."),
                new FrameworkBinding("transparency-disclosure", "A.8.5",
                        "Provenance fields per event satisfy 'information for users' requirements."),
                new FrameworkBinding("incident-classification", "A.8.4",
                        "Severity-tagged audit events support incident communication to interested parties."),
                new FrameworkBinding("purpose-binding", "A.9.2",
                        "purpose_code enforcement keeps invocations within documented intended use."),
                new FrameworkBinding("third-party-risk", "A.10.3",
                        "Model registry entries treat LLM providers as managed third-party suppliers."),
                new FrameworkBinding("data-residency", "A.6.2.4",
                        "Residency-by-construction is a deployment-criteria control."),
                new FrameworkBinding("dual-control-kill-switch", "A.6.2.8",
                        "Kill switch is a controlled change-management primitive for stopping production agents.")
        );
    }
}
