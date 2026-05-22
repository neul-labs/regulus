/**
 * Runtime Identity-related guards that run as ADK plugins. The first guard
 * in the BeforeModel chain is
 * {@link com.neullabs.regulus.adk.plugins.guard.RegulusIdentityExpiryGuard},
 * which short-circuits any request bound to an expired
 * {@link com.neullabs.regulus.identity.Identity}.
 */
package com.neullabs.regulus.adk.plugins.guard;
