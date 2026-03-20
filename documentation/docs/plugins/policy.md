# RegulusPolicyPlugin

## In one sentence

Enforces declarative policy guards on every ADK agent invocation: purpose
binding, consent, GDPR Art. 22 safeguards, Consumer Duty outcomes,
vulnerable-customer routing.

## Who does it apply to?

Any agent under any Regulus profile. The plugin is profile-aware — it reads
`ComplianceProfile.controls()` and activates whichever guards the profile
declares.

## The two-minute explainer

Policy guards are the *first* control point in the request lifecycle: before
the model is called, before any tool fires, the policy decider gets to say
"allow," "block," or "require confirmation."

The decision is driven by the active profile's `ControlBinding`s. If the
profile says `purpose-binding` is required (GDPR / UK GDPR), the plugin
refuses any call missing a `purposeCode`. If the profile says
`automated-decisions-safeguards` is required (GDPR Art. 22, AI Act Art. 14),
the plugin routes flagged calls through ADK's `ToolConfirmation` HITL flow
before the model is consulted. If the profile says
`vulnerable-customer-handling` is required (FCA Consumer Duty FG22/5 §4),
flagged customers get the same HITL treatment.

The plugin is **pure ADK** — `BasePlugin` + `BeforeModelCallback` +
`BeforeToolCallback`. No Spring, no AOP.

## What it actually requires of an engineer

To use the plugin, you need to:

- Activate at least one profile with `regulus.compliance.profiles: [...]`.
- Pass an invocation context that includes the fields the active guards
  consume (`purposeCode`, `vulnerable_customer`, `automated_legal_effect`,
  etc.). The Spring starter wires this from request headers; bare-Java
  callers populate `PolicyContext` themselves.

## What Regulus does for you

- Maps each profile's `ControlBinding` to a runtime check (in
  `DefaultPolicyEngine`).
- Emits structured `PolicyDecision` outputs: `Allow`, `Block`,
  `RequireConfirmation`.
- Surfaces clause citations on every block so the audit trail and the
  developer console both know *which clause* failed.
- Composes profiles correctly: a request that violates *any* active
  profile's binding is blocked, with the offending clause cited.

## Saves you ~

- ~2 engineer-weeks for a YAML-driven policy engine that compiles.
- ~1 engineer-week to wire `ToolConfirmation` correctly into ADK's HITL
  flow.
- ~1 engineer-week to keep citations in sync with regulation updates.

Net: ~4 engineer-weeks.

## Code: minimal

```java
RegulusPolicyPlugin policy = RegulusPolicyPlugin.fromProfile(
    ComplianceProfiles.byId("uk-gdpr"));
App app = App.builder("my-agent", rootAgent).plugin(policy).build();
```

## Code: production

With the Spring starter:

```yaml
regulus:
  compliance:
    profiles: [uk-gdpr, fca-sysc]
```

The plugin auto-configures and consumes the composite profile.

To customise the decider (e.g. add bespoke guards for a tenant):

```java
PolicyDecider decider = context -> {
    if ("forbidden-tool".equals(context.targetId()) && "tool".equals(context.targetKind())) {
        return new PolicyDecision.Block("tool_blocked",
            "This tool is disabled for the tenant", "tenant-policy");
    }
    return DefaultPolicyEngine.evaluate(profile, context);
};
RegulusPolicyPlugin custom = RegulusPolicyPlugin.withDecider(profile, decider);
```

## How to verify

- Send a request without `purpose_code` under the `gdpr` profile → block
  with citation `Art. 5(1)(b)`.
- Send a request with `automated_legal_effect=true` → `RequireConfirmation`
  with citation `Art. 22`.
- Send a request with `vulnerable_customer=true` under `fca-sysc` →
  `RequireConfirmation` with citation `FG22/5 §4`.

## What an auditor will ask

1. **"Show me a policy block."** Pull an audit event with `result=block`.
2. **"Why this clause?"** The event's `clause_citation` field.
3. **"How is the HITL approver chosen?"** Operational — your runbook + ADK
   `ToolConfirmation` configuration.

## What this doesn't cover

- **Policy authoring DSL.** YAML / Kotlin DSL parsers live in
  `regulus-ai-dsl-yaml` and `regulus-ai-dsl-kotlin` and feed
  `RegulusPolicyPlugin.withDecider`.
- **Tenant-bespoke guards.** Implement a `PolicyDecider`.
- **The actual ToolConfirmation runtime.** That's ADK's primitive — we
  return `RequireConfirmation`; ADK does the user-prompt + resume.

## Citations

- See the regulation-specific compliance pages for clause references:
  - [GDPR](../compliance/eu/gdpr.md)
  - [UK GDPR](../compliance/uk/uk-gdpr.md)
  - [EU AI Act](../compliance/eu/eu-ai-act.md)
  - [FCA SYSC](../compliance/uk/fca-sysc.md)
