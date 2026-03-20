# RegulusComplianceBaseComputer

## In one sentence

A reference compliant implementation of ADK's `BaseComputer` interface,
used by `ComputerUseTool` — closes the gap Google explicitly flagged when
they shipped the abstraction.

## What it does

ADK's `ComputerUseTool` lets an agent drive a browser or computer. Google
ships the *tool* but says developers will have to provide their own
`BaseComputer` implementation. Regulus ships one with compliance built in:

- **Action audit.** Every click, type, navigate, screenshot lands in the
  audit trail.
- **Domain allowlist.** The agent can only navigate to domains the
  tenant has approved.
- **Screenshot PII redaction.** Built-in patterns are applied to OCR'd
  text in screenshots before they leave the executor.
- **HITL on high-risk actions.** `FORM_SUBMIT`, `PAYMENT_CONFIRM`,
  `FILE_DOWNLOAD`, `FILE_UPLOAD`, `LOGIN_CREDENTIAL_ENTRY` route through
  ADK `ToolConfirmation`.

## When to use it

Whenever you wire `ComputerUseTool` into a Regulus-managed agent.

## Code

```java
RegulusComplianceBaseComputer computer = new RegulusComplianceBaseComputer(
    Set.of("portal.example.com", "auth.example.com"),
    /* redactScreenshots = */ true,
    Set.of(
        RegulusComplianceBaseComputer.HighRiskAction.FORM_SUBMIT,
        RegulusComplianceBaseComputer.HighRiskAction.PAYMENT_CONFIRM,
        RegulusComplianceBaseComputer.HighRiskAction.FILE_DOWNLOAD,
        RegulusComplianceBaseComputer.HighRiskAction.LOGIN_CREDENTIAL_ENTRY
    ));
```

Wire into `ComputerUseTool`:

```java
ComputerUseTool tool = ComputerUseTool.builder()
    .computer(computer)
    .build();
```

## Failure modes

- Out-of-allowlist navigation → blocked + audit event.
- High-risk action without confirmation → suspends, requests
  `ToolConfirmation`.
- Screenshot OCR finds PII → redacted before audit emission.

## What this doesn't cover

- The underlying browser automation. Pick Playwright / Selenium / Chrome
  DevTools.
- OCR quality. We use a standard OCR pipeline; tune as needed.
- Visual deepfake / spoofing detection. Out of scope.
