# Security model

In regulated AI, "who is doing this?" is the first question every audit log
answers — and "who" is not just a user ID. Without a clear model of the
caller, the audit trail is a list of strings the inspector can't reconcile,
the residency policy can't tell EU traffic from UK traffic, and dual control
becomes "two arbitrary names in a database."

Regulus defines a canonical identity primitive so every part of the system —
policy guards, audit, A2A, kill-switch — reads from the same shape, no
matter which protocol the caller authenticated with.

## What "identity" means in regtech

For a regulator, *identity* answers six questions in one breath:

1. **Subject.** Who is the principal — a human, a service account, an
   autonomous agent acting on a human's behalf?
2. **Tenant.** Which customer / firm / legal entity does the principal
   belong to? Residency, retention, and risk tiers are tenant-scoped.
3. **Jurisdiction.** EU, UK, or both? Drives which compliance profile
   evaluates the call (GDPR vs UK GDPR vs the composite).
4. **Purpose.** Which `purpose_code` is the call authorised for?
   GDPR Art. 5(1)(b) is the load-bearing clause here — agents cannot
   run "for any purpose."
5. **Lawful basis.** Which Art. 6 (or Art. 9) ground does the processing
   rely on? Consent? Legitimate interest? Public task?
6. **Roles.** What's the principal allowed to *do* — request a kill
   switch, approve one, change a policy, see redacted data?

A bare OAuth bearer token answers question 1. Maybe question 6 if you
squint. The other four are usually scattered across custom claims,
session attributes, and tribal knowledge.

Regulus' job is to make all six legible to the audit trail and to every
policy guard, on every request.

## The canonical primitive: Principal and Claims

```java
record Principal(String id, String displayName, PrincipalType type) {}
record Claims(
    String tenantId,
    Jurisdiction jurisdiction,
    Set<String> purposeCodes,
    Set<String> roles,
    Set<String> lawfulBases,
    Map<String,String> extensions) {}
record Identity(Principal principal, Claims claims, Provenance provenance) {}
```

That's the whole canonical model. Every adapter — OIDC, SAML, mTLS, a
service-account JWT — mints one of these. Downstream code reads only from
here. There is no second source of truth.

`Provenance` records which adapter produced the Identity, when it was
minted, when the underlying token expires, and which issuer signed it.
That metadata is what makes audit events traceable back to the
authentication event that authorised them.

For the field-by-field shape, the SPI signatures, and the trust model in
detail, see [Security architecture](../advanced/security-architecture.md).

## Adapters: how external identity becomes a Principal

Every enterprise IdP is bespoke. Okta and Auth0 lay out claims one way,
Keycloak another, ADFS yet another, an internal mTLS scheme a fourth.
Hard-coding one of these into Regulus would lock tenants out the door.

So Regulus refuses to ship a one-size-fits-all login. Instead, an
**`IdentityAdapter`** SPI takes whatever shape the IdP produces and mints
the canonical `Identity`. The reference adapter — `OidcIdentityAdapter` —
maps Spring Security's `JwtAuthenticationToken` and ships in a separate
starter so non-OIDC tenants don't pay for Spring Security on the
classpath.

Custom adapters are a few dozen lines. The `SAML adapter` example in
[Security architecture](../advanced/security-architecture.md#identityadapter-spi)
sketches one.

## Trust boundaries at a glance

The system has five trust boundaries; each has its own answer to "what is
trusted across this edge?":

- **Caller → Spring Boot HTTP.** Trusted via IdP-issued JWT (signature,
  expiry, audience).
- **Spring Boot → ADK runtime.** In-process; the already-minted Identity
  is propagated via a thread-local holder.
- **ADK runtime → Regulus plugins.** In-process; plugin code is trusted.
- **Local agent → remote A2A agent.** Network; verified by RFC 9421 HTTP
  Message Signatures over the caller's Identity.
- **Operator → kill-switch.** Authorisation by Identity role, with
  dual-control on deactivation.

For the diagram and the failure modes when an edge breaks, see
[Security architecture → Trust boundaries](../advanced/security-architecture.md#trust-boundaries).

## How this connects to audit and dual control

Every audit event is stamped with the caller's `Principal.id` and the
canonical claims that authorised the call. The audit trail and the
identity model are the same story told twice — once on the inbound side,
once in the persisted log.

Dual control (see [Dual control / 4-eyes](dual-control.md)) is
authorisation over **Principals**, not strings. The kill-switch
authorizer checks the approver's roles, and the approver-distinctness
rule compares `Principal.id` so two distinct subjects are required, not
two distinct typed names.

## What Regulus does not do

Regulus is not your IdP. Specifically:

- It does **not** authenticate users — your IdP does. Regulus consumes
  the result.
- It does **not** manage user lifecycle, password reset, MFA enrolment,
  or session timeout. Those live in Okta, Auth0, Keycloak, or your
  in-house equivalent.
- It does **not** store credentials. The closest thing Regulus stores is
  a key id for signing audit chains and A2A envelopes.
- It does **not** issue tokens. It verifies and consumes them.

The mental model: your IdP is the airport's check-in desk. Regulus is
border control on the airside — it doesn't know how you got the
passport, but it knows what to do with it once you're past the gate.

## Where next

- [Security architecture](../advanced/security-architecture.md) —
  threat model, SPI signatures, trust-boundary diagram, A2A signing,
  audit integrity, kill-switch authorisation, failure modes.
- [Audit trails](audit-trails.md) — why Identity is the first thing
  every audit event carries.
- [Controller, processor, deployer](controller-processor-deployer.md) —
  the regulatory roles encoded into `Claims`.
