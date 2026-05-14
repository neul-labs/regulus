# 1L — Engineering line

The people who build and run the AI agent. In a Regulus deployment that's
the team writing the ADK `App`, owning the Spring Boot service, paging on
incidents.

## What this line owns

- The AI system's correct operation.
- Initial quality, data integrity, technical controls.
- First-line incident response.
- Documentation of how the system works.

## What Regulus gives them

**Runtime guardrails embedded as code.** No separate process to remember;
the plugins fire on every invocation.

| Need | Regulus mechanism |
|---|---|
| Stop PII reaching the model | `RegulusPrivacyPlugin` |
| Enforce purpose / consent | `RegulusPolicyPlugin` |
| Halt agent fast under incident | `RegulusKillSwitchPlugin` |
| Gate by model risk tier | `RegulusModelRiskPlugin` |
| Stop accidental region drift | `RegulusDataResidencyPlugin` |
| Audit every action | `RegulusAuditPlugin` |
| Apply to agent-to-agent calls | `regulus-ai-adk-a2a` |

## Daily life on this line

- **Build new agents** with the standard plugin stack from the BOM.
- **Read audit events** to debug regressions and verify behaviour.
- **Operate the kill switch** as a first response when something goes
  wrong (single-control activation; deactivation pairs with another
  operator).
- **Onboard new tools** through model registry + policy guard updates.

## What 1L does *not* own

- Defining the policy. 2L (with legal) drafts it; 1L enforces it.
- Validating the model. 2L's model-risk function does that.
- Determining tier. 2L's risk function classifies; 1L enforces the
  classification via `RegulusModelRiskPlugin`.

## Boundary signals

When 1L is bleeding into 2L territory, you typically see one of:

- An engineer hand-waving "we'll just add a special case for this
  customer" — should be a policy update, not a code patch.
- A team flipping the kill switch unilaterally and unflipping it
  unilaterally — dual-control deactivation exists for a reason.
- Audit events with missing or self-assigned `smf_holder` — escalate.

Regulus surfaces these as audit events but doesn't decide. That's
deliberately 2L's job.
