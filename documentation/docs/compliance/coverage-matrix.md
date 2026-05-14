# Coverage matrix

How each Regulus mechanism maps to each shipped profile, with the ADK hook
it uses. Regenerate with `./gradlew regulusComplianceMatrix`.

## How to read this

- **Mechanism** — the Regulus control name (matches
  `ControlBinding.mechanism`).
- **Profile clauses** — which articles / paragraphs each profile cites the
  mechanism against.
- **ADK hook** — the official ADK extension point Regulus implements it
  through.
- **Test fixture** — where the unit test for this mechanism lives.

## Matrix

| Mechanism | EU AI Act | GDPR / UK GDPR | DORA | NIS2 | FCA SYSC | PRA SS1/23 | PRA SS2/21 | NHS DSPT | EHDS | ADK hook | Test fixture |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `purpose-binding` | — | Art. 5(1)(b) | — | — | — | — | — | — | — | `BeforeModelCallback` (PolicyPlugin) | `DefaultPolicyEngineTest#purposeBinding` |
| `automated-decisions-safeguards` | — | Art. 22 | — | — | — | — | — | — | — | `BeforeModelCallback` (PolicyPlugin) + `ToolConfirmation` | `DefaultPolicyEngineTest#art22Safeguard` |
| `privacy-by-design` | — | Art. 25 | — | — | — | — | — | — | — | `BeforeModelCallback` (PrivacyPlugin) | `RegulusPrivacyPluginTest#redact` |
| `pii-redaction` | — | Art. 5(1)(c) | — | — | — | — | — | DSPT 1.x | — | `BeforeModelCallback` + `AfterModelCallback` (PrivacyPlugin) | `BuiltInPatternsTest` |
| `storage-limitation` | Art. 19 | Art. 5(1)(e) | Art. 12 | — | SYSC 9 | — | — | NHS records | EHDS retention | `EventCompactor` (RetentionEventCompactor) | `RegulusRetentionEventCompactorTest` |
| `audit-trail` | Art. 12 | Art. 30 | Art. 12 | Art. 23 | SYSC 9 | SS1/23 §2-5 | SS2/21 §7 | DSPT 6.x | Art. 56 | `After*Callback` (AuditPlugin) | `RegulusAuditPluginTest` |
| `cross-border-residency` | — | Arts. 44-49 | Art. 28 | — | SYSC 13 | — | SS2/21 §6 | DSPT 8.x | Chapter III | Startup + `BeforeAgentCallback` (DataResidencyPlugin) | `RegulusDataResidencyPluginTest` |
| `dual-control-kill-switch` | Art. 14 | — | — | — | (FG22/5) | SS1/23 §6 | — | — | — | `BeforeAgentCallback` (KillSwitchPlugin) + `ToolConfirmation` | `InMemoryKillSwitchStoreTest` |
| `model-risk-tier` | Art. 9, Annex III | — | — | — | SYSC 4 | SS1/23 §3 | — | — | — | `BeforeModelCallback`, `BeforeToolCallback` (ModelRiskPlugin) | `RegulusModelRiskPluginTest` |
| `transparency-disclosure` | Art. 13 | — | — | — | — | — | — | — | — | `AfterModelCallback` (AuditPlugin) | `RegulusAuditPluginTest#transparency` |
| `post-market-monitoring` | Art. 16 | — | — | — | — | SS1/23 §5 | — | — | — | `AfterAgentCallback` (AuditPlugin) + observability | (integration) |
| `deployer-obligations` | Art. 26 | — | — | — | — | — | — | — | — | `BeforeAgentCallback` (KillSwitchPlugin + PolicyPlugin) | (integration) |
| `incident-classification` | — | Art. 33 | Arts. 17-23 | Art. 23 | — | — | — | DSPT 6.x | — | `AfterAgentCallback` (AuditPlugin) | `RegulusAuditPluginTest#incidentSeverity` |
| `third-party-risk` | — | — | Arts. 28-30 | Art. 21(2)(d) | SYSC 13 | — | SS2/21 §3 | — | — | Model registry + audit linkage | (integration) |
| `consumer-duty-good-outcomes` | — | — | — | — | FG22/5 | — | — | — | — | `BeforeModelCallback` (PolicyPlugin) | `DefaultPolicyEngineTest#consumerDuty` |
| `vulnerable-customer-handling` | — | — | — | — | FG22/5 §4 | — | — | — | — | `BeforeModelCallback` (PolicyPlugin) + `ToolConfirmation` | `DefaultPolicyEngineTest#vulnerableCustomer` |
| `senior-management-arrangements` | — | — | — | — | SYSC 4 | — | — | — | — | Audit attribution (`smf_holder`) | `RegulusAuditPluginTest#smfAttribution` |
| `model-inventory` | — | — | — | — | — | SS1/23 §2 | — | — | — | Model registry | `ModelRegistryTest` |
| `kill-switch-readiness` | Art. 14 | — | — | — | — | SS1/23 §6 | — | — | — | `BeforeAgentCallback` (KillSwitchPlugin) | `RegulusKillSwitchPluginTest` |
| `personal-data-protection` | — | — | — | — | — | — | — | DSPT 1.x | — | `BeforeModelCallback` (PrivacyPlugin) | `BuiltInPatternsTest#nhsNumber` |
| `clinician-identity` | — | — | — | — | — | — | — | DSPT 4.x | — | Audit attribution (`clinician_smartcard_id`) | `RegulusAuditPluginTest#clinicianIdentity` |
| `secondary-use-permit` | — | — | — | — | — | — | — | — | Chapter IV | Audit attribution (`permit_ref`) | (integration) |
| `data-quality-labels` | — | — | — | — | — | — | — | — | Art. 56 | `AfterModelCallback` (AuditPlugin) | (integration) |

## Framework bindings

The same Regulus mechanisms above also bind to **governance framework**
control ids. Activate via `regulus.governance.frameworks: [...]`.

| Mechanism | NIST AI RMF | NIST 600-1 GenAI | NIST Agent Interop (planned) | ISO/IEC 42001 | ISO/IEC 23894 | ISO/IEC 23053 |
|---|---|---|---|---|---|---|
| `purpose-binding` | MAP-1.1 | — | — | A.9.2 | — | — |
| `automated-decisions-safeguards` | — | GAI-7 | — | — | — | — |
| `privacy-by-design` | — | GAI-4 | — | A.7 | — | — |
| `pii-redaction` | — | GAI-4 | — | A.7.3 | — | — |
| `storage-limitation` | GOVERN-1.5 | — | — | A.6.2.7 | — | — |
| `audit-trail` | GOVERN-1.5, MEASURE-1.1 | GAI-8 | AGENT-MONITORING-1 | A.6.2.7 | CL-6.7 | CL-6.5 |
| `cross-border-residency` | MEASURE-2.7 | GAI-9 | — | A.6.2.4 | — | — |
| `dual-control-kill-switch` | GOVERN-4.1 | GAI-2 | AGENT-SECURITY-2 | A.6.2.8 | CL-6.6 | — |
| `model-risk-tier` | MAP-4.1 | GAI-7 | AGENT-SECURITY-1 | A.5.2 | CL-6.3, CL-6.4 | CL-7 |
| `transparency-disclosure` | MEASURE-2.8 | GAI-7 | — | A.8.5 | — | — |
| `post-market-monitoring` | MANAGE-4.1 | — | — | A.6.2.5 | — | — |
| `incident-classification` | MANAGE-2.2 | — | — | A.8.4 | — | — |
| `third-party-risk` | GOVERN-6.1 | GAI-12 | — | A.10.3 | — | — |
| `senior-management-arrangements` | GOVERN-2.1 | — | — | A.3.2 | — | — |
| `model-inventory` | — | — | — | A.4.4 | — | CL-6.2 |
| `policy-engine` | GOVERN-1.1 | GAI-3 | AGENT-IDENTITY-2 | A.2.2 | — | — |
| `a2a-envelope` | — | — | AGENT-IDENTITY-1, AGENT-MONITORING-2 | — | — | — |

Bindings are generated from each `GovernanceFramework.bindings()`. The
NIST AI RMF Agent Interop Profile column uses provisional IDs from the
April 2026 concept note; final IDs land when NIST publishes (target Q4
2026).

## Notes

- Empty cells mean the mechanism is not bound to a specific clause of that
  profile. A cell with `—` means the regulation doesn't require this
  control — Regulus may still emit it for defence in depth.
- Profiles bind mechanisms to specific *clauses* via `ControlBinding`. The
  same mechanism can be cited against different clauses in different
  profiles.
- The composite profile (built from
  `regulus.compliance.profiles: [...]`) unions all bindings.
- The composite framework (built from
  `regulus.governance.frameworks: [...]`) does the equivalent for
  frameworks; both join into the same audit event via
  `regulation_clause` and `framework_control_id` fields.
