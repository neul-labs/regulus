package com.regulus.platform.governance.framework;

import com.regulus.platform.governance.FrameworkBinding;
import com.regulus.platform.governance.FrameworkControl;
import com.regulus.platform.governance.FrameworkKind;
import com.regulus.platform.governance.GovernanceFramework;

import java.util.Set;

/**
 * NIST AI 600-1 — Generative AI Profile (July 2024).
 *
 * <p>Maps 12 generative-AI-specific risks to actions across the AI RMF's
 * four functions. Regulus binds its mechanisms to the GAI risks they most
 * directly address.
 */
public final class NistAiRmfGenAiProfile implements GovernanceFramework {

    @Override public String id()           { return "nist-ai-rmf-600-1"; }
    @Override public String displayName()  { return "NIST AI RMF Generative AI Profile (600-1)"; }
    @Override public String version()      { return "AI 600-1 (July 2024)"; }
    @Override public FrameworkKind kind()  { return FrameworkKind.VOLUNTARY; }
    @Override public String authorityUrl() { return "https://nvlpubs.nist.gov/nistpubs/ai/NIST.AI.600-1.pdf"; }

    @Override public Set<FrameworkControl> controls() {
        return Set.of(
                new FrameworkControl("GAI-1", "Risk", "CBRN information or capabilities",
                        "GAI may lower barriers to entry for designing or deploying CBRN weapons."),
                new FrameworkControl("GAI-2", "Risk", "Confabulation",
                        "GAI confidently produces erroneous outputs that mislead users."),
                new FrameworkControl("GAI-3", "Risk", "Dangerous, violent, or hateful content",
                        "GAI generates content that is dangerous, violent, or hateful."),
                new FrameworkControl("GAI-4", "Risk", "Data privacy",
                        "GAI exposes personal or sensitive information through model outputs or training data."),
                new FrameworkControl("GAI-5", "Risk", "Environmental impact",
                        "GAI consumes significant resources during training and inference."),
                new FrameworkControl("GAI-6", "Risk", "Harmful bias or homogenization",
                        "GAI outputs reflect or amplify harmful bias from training data."),
                new FrameworkControl("GAI-7", "Risk", "Human-AI configuration",
                        "Risks from how humans interact with GAI: anthropomorphization, over-reliance, automation complacency."),
                new FrameworkControl("GAI-8", "Risk", "Information integrity",
                        "GAI may produce false or misleading information that harms public discourse."),
                new FrameworkControl("GAI-9", "Risk", "Information security",
                        "GAI may produce content that enables cyberattacks or amplify attacker capabilities."),
                new FrameworkControl("GAI-10", "Risk", "Intellectual property",
                        "GAI may produce content that infringes IP, or use protected works in training."),
                new FrameworkControl("GAI-11", "Risk", "Obscene, degrading, or abusive content",
                        "GAI may produce CSAM or non-consensual intimate imagery."),
                new FrameworkControl("GAI-12", "Risk", "Value chain and component integration",
                        "Risks arising from third-party GAI components and integration into larger systems.")
        );
    }

    @Override public Set<FrameworkBinding> bindings() {
        return Set.of(
                new FrameworkBinding("pii-redaction", "GAI-4",
                        "RegulusPrivacyPlugin masks PII before it reaches the model — addresses data privacy at the inference boundary."),
                new FrameworkBinding("audit-trail", "GAI-8",
                        "Per-invocation audit with model_id, model_version, and content provenance fields supports information integrity claims."),
                new FrameworkBinding("dual-control-kill-switch", "GAI-2",
                        "When confabulation is detected at scale, the kill switch allows immediate halt with dual-control oversight."),
                new FrameworkBinding("model-risk-tier", "GAI-7",
                        "Tier-aware policy restricts the model classes available for high-risk human-AI configurations."),
                new FrameworkBinding("third-party-risk", "GAI-12",
                        "Model registry treats each LLM provider as an explicit third-party component with documented integration."),
                new FrameworkBinding("policy-engine", "GAI-3",
                        "Policy guards can deny invocations whose context suggests dangerous-content generation paths."),
                new FrameworkBinding("data-residency", "GAI-9",
                        "Residency pinning constrains the attack surface to the regions the firm controls."),
                new FrameworkBinding("transparency-disclosure", "GAI-7",
                        "Audit provenance fields support user-facing 'you are interacting with AI' transparency obligations.")
        );
    }
}
