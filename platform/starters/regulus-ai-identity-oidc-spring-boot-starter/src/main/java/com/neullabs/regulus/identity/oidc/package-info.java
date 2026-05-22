/**
 * Reference OIDC adapter. Maps Spring Security's
 * {@code JwtAuthenticationToken} into a Regulus
 * {@link com.neullabs.regulus.identity.Identity} on every authenticated
 * request and binds it to {@code IdentityHolder} for the request thread.
 *
 * <p>This starter is opt-in. Spring Security is a {@code compileOnly}
 * dependency, so non-OIDC tenants are unaffected. The autoconfig is
 * dormant unless {@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken}
 * is on the runtime classpath.
 */
package com.neullabs.regulus.identity.oidc;
