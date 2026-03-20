# adk-multi-agent-a2a — Regulus envelope on A2A

Two ADK agents — an `intake-agent` and a `decision-agent` — talking over the
official ADK A2A (Agent-to-Agent) JSON-RPC protocol. Both wear the Regulus
envelope, so every hop is policy-checked, redacted, audited, and respects the
kill switch.

This is the deployment shape most large teams end up with: agents specialise,
talk to each other, and compliance has to follow the data — not stop at the
edge.

## Run it

```bash
./gradlew :examples:adk-multi-agent-a2a:bootRun
```

You'll see two agents register, each with its own Regulus plugin stack. A
sample call into `intake-agent` triggers a downstream call to `decision-agent`
over A2A; the audit log shows:

```
event=a2a-inbound  envelope=ok  caller=intake-agent       redactions=[NINO_1]
event=a2a-outbound envelope=ok  callee=decision-agent     redactions=[NINO_1]
event=model-call   model=gemini-2.5-flash tier=STANDARD
event=a2a-response envelope=ok  callee=decision-agent     redactions=[NINO_1]
```

Note the `NINO_1` token threads through the whole graph — Regulus never lets
the raw value leave the JVM, even across the A2A hop.

## What this shows

- `RegulusAgentExecutor` wraps the inbound side of A2A JSON-RPC.
- `RegulusRemoteA2AAgent` wraps the outbound side.
- Both share the same `AuditSink`, so an auditor sees one continuous trail
  per request — not one trail per agent.

## Where to read next

- [A2A reference](../../documentation/docs/services/a2a.md)
- [Operations — kill-switch playbook](../../documentation/docs/operations/kill-switch-playbook.md)
