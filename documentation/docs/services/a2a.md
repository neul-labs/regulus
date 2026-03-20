# A2A envelope (`regulus-ai-adk-a2a`)

## In one sentence

Wraps ADK's `AgentExecutor` (inbound) and `RemoteA2AAgent` (outbound) so
every Agent-to-Agent hop carries the Regulus envelope: policy, privacy,
audit, kill-switch.

## Why this matters

Big production deployments end up with multiple agents calling each other.
Without the A2A envelope, compliance stops at each agent's local edge —
a redacted prompt from agent A can be sent unredacted to agent B over the
A2A JSON-RPC. The envelope plugs that hole.

## What it does

- **Inbound (`RegulusAgentExecutor`).** Wraps the JSON-RPC handler:
  policy + privacy + kill-switch checks run on the inbound request before
  it reaches the agent. An audit event with `a2a-inbound` is emitted.
- **Outbound (`RegulusRemoteA2AAgent`).** Wraps the remote-call shim:
  the request is policy-checked, privacy-redacted, audit-emitted before
  leaving the JVM. Optionally signed for cross-org A2A interop where the
  receiving party wants to verify provenance.

## When to use it

Whenever your agents talk to other agents — local or remote, same org or
cross-org.

## Code

```java
RegulusAgentExecutor executor = new RegulusAgentExecutor(
    auditSink, /* signRequests = */ true);

RegulusRemoteA2AAgent remote = new RegulusRemoteA2AAgent(
    URI.create("https://agent.partner.example.com/a2a"),
    auditSink,
    /* signRequests = */ true);
```

## What this doesn't cover

- **Identity federation across orgs.** Use OIDC / mTLS at the transport
  layer.
- **Distributed tracing.** Compatible with OpenTelemetry; add the spans
  in your transport.
- **Schema evolution of A2A messages.** Tracks ADK's protocol; we update
  with ADK releases.

## See also

- [`RegulusAuditPlugin`](../plugins/audit.md)
- [`RegulusPolicyPlugin`](../plugins/policy.md)
- [Example: `adk-multi-agent-a2a`](https://github.com/Skelf-Research/regulus/tree/main/examples/adk-multi-agent-a2a)
