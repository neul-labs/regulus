/**
 * Regulus canonical identity model. Every external identity protocol (OIDC, SAML,
 * mTLS, service-account JWT) plugs in as an {@link com.neullabs.regulus.identity.spi.IdentityAdapter}
 * that mints the same internal {@link com.neullabs.regulus.identity.Identity}: a
 * {@link com.neullabs.regulus.identity.Principal} plus regulator-shaped
 * {@link com.neullabs.regulus.identity.Claims} (tenant, jurisdiction, purpose codes,
 * lawful bases, roles) plus {@link com.neullabs.regulus.identity.Identity.Provenance}.
 *
 * <p>This module is a true leaf: no Spring, no AOP. Audit, A2A, kill-switch and
 * policy modules can all import it without cycles.
 */
package com.neullabs.regulus.identity;
