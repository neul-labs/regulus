# Your first compliant agent

You already have an ADK agent. This page shows how to drop Regulus in
without ripping anything out.

## Starting point

Assume a working ADK + Spring Boot agent like the one in the [ADK
quickstart](adk-quickstart.md) — or an existing app of your own.

## 1. Add the dependencies

```kotlin
implementation(platform("com.neullabs:regulus-ai-bom:0.1.0"))
implementation("com.neullabs:regulus-ai-adk-spring-boot-starter")
```

## 2. Add the configuration

```yaml
regulus:
  compliance:
    profiles: [uk-gdpr, fca-sysc]    # pick the ones that apply to you
  adk:
    audit:
      sink: stdout                    # kafka for prod
    residency:
      allowed-regions: [europe-west2]
    kill-switch:
      enabled: true
      dual-control: true
    model-risk:
      tenant-tier: REGULATED
```

Run the app. Six Regulus plugins are now active.

## 3. Tag your requests

The plugins consume context fields. Populate them on the inbound side of
your application — typically a Spring filter or interceptor:

```java
@Component
class RegulusContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        // pull these from your auth context, headers, or session:
        PolicyContextHolder.set(new PolicyContext(
            http.getHeader("X-Purpose-Code"),
            http.getHeader("X-Subject-Id"),
            "user:" + http.getRemoteUser(),
            "model",
            null,
            Map.of(
                "smf_holder", http.getHeader("X-SMF-Holder"),
                "consumer_duty_outcome", http.getHeader("X-Consumer-Duty-Outcome"),
                "vulnerable_customer", Optional.ofNullable(http.getHeader("X-Vulnerable")).orElse("false")
            )));
        try { chain.doFilter(req, res); } finally { PolicyContextHolder.clear(); }
    }
}
```

(`PolicyContextHolder` is the Regulus-provided ThreadLocal binding for
non-async paths; for reactive / virtual-threads paths use the
`ContextPropagation` integration documented under Operations.)

## 4. Watch the audit log

You should now see structured events for each request:

```
{
  "event_id": "...",
  "actor": "user:42",
  "purpose_code": "mortgage-advice",
  "smf_holder": "SMF24:Jane Smith",
  "consumer_duty_outcome": "support",
  "model_id": "gemini-2.5-flash",
  ...
}
```

## 5. Test failure modes

- Strip the `X-Purpose-Code` header → request blocked with
  `Block(missing_purpose, ..., Art. 5(1)(b))`.
- Set `X-Vulnerable: true` → request routed through `RequireConfirmation`.
- Try to call a `REGULATED`-tier model from a `STANDARD` tenant → blocked.

## What changed

The agent's behaviour didn't change — it still does its work. What
changed is the *envelope*: every action is policy-checked, redacted,
audited, and residency-pinned. If a regulator turns up tomorrow, every
question has an artefact to point at.

## Next

- [Multi-agent with A2A](multi-agent-a2a.md) — extend the envelope across
  agent hops.
- [Deploy to Vertex AI Agent Engine](vertex-deploy.md) — production hardening.
- [Compliance pages](../compliance/index.md) — pick more profiles.
