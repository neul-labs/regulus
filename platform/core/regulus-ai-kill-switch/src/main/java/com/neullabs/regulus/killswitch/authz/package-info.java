/**
 * Identity-backed authorization for kill-switch operations. Whether a caller
 * can request, approve, or emergency-bypass an activation is decided from
 * their canonical {@link com.neullabs.regulus.identity.Identity} roles, not
 * an opaque user-id string.
 *
 * <p>Default canonical roles:
 * <ul>
 *   <li>{@code regulus.killswitch.requester} — can request activation/deactivation</li>
 *   <li>{@code regulus.killswitch.approver}  — can approve a pending request</li>
 *   <li>{@code regulus.killswitch.emergency} — can invoke the emergency bypass</li>
 * </ul>
 *
 * <p>Role names are configurable via {@code regulus.kill-switch.roles.*}
 * so enterprise tenants can map to their existing IdP role taxonomy
 * without forking.
 */
package com.neullabs.regulus.killswitch.authz;
