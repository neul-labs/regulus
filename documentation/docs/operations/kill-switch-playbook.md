# Kill-switch playbook

When and how to operate the dual-control kill switch.

## When to activate

Any of:

- A model is producing harmful outputs at scale.
- A downstream system is in a failure mode the agent can amplify.
- A regulator instructs you to halt.
- A security incident affects the agent.

Activation is **fast** and **unilateral**. The slowdown — and the
controls — kick in when you turn it *off*.

## Activation procedure

1. Identified, authorised operator hits the admin endpoint:
   ```
   POST /admin/kill-switch
   { "scope": "agents/<name>", "operator": "<ops-A>", "reason": "<one line>" }
   ```
2. All in-flight requests fail. New requests get `KillSwitchActive`.
3. Audit event emitted.
4. Notify stakeholders (Slack channel, paging system).

## Deactivation procedure

1. Operator B proposes deactivation:
   ```
   POST /admin/kill-switch/deactivate-request
   { "scope": "agents/<name>", "operator": "<ops-B>", "reason": "<one line>" }
   ```
   The system records the request; the switch stays active.
2. Operator C (different person) confirms:
   ```
   POST /admin/kill-switch/deactivate-confirm
   { "scope": "agents/<name>", "operator": "<ops-C>" }
   ```
3. System verifies C ≠ B; if so, the switch lifts.
4. Audit event records all three operators + timestamps.

## Quarterly drill

1. Schedule a 30-minute window.
2. Rotate operators each quarter — don't use the same two every time.
3. Drill the full flow: activate, request-deactivate-by-same-operator
   (refused), proper confirmation by a different operator.
4. Walk the audit trail afterwards. Capture any anomalies into the
   issue tracker.

## Post-incident

After any real activation:

- Same-day debrief.
- One-week written incident report linking to the audit trail.
- One-month follow-up: were corrective actions taken?

## Common mistakes

- **Letting the same two operators handle every drill.** Defeats the
  point. Rotate.
- **Activation without a clearly recorded reason.** Audit shows the
  string you typed; make it count.
- **Forgetting to notify stakeholders.** Activation triggers a Slack
  webhook; verify it actually fires in drills.
