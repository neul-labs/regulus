package com.neullabs.regulus.identity;

/**
 * Regulatory jurisdiction that scopes the request. Drives which compliance
 * profile evaluates the call (GDPR vs UK GDPR vs both) and which residency
 * allowlist applies.
 *
 * <p>This is the canonical home for the enum. {@code regulus-ai-compliance}
 * historically defined the same type and now re-exports this one for one
 * release to keep callers compiling during the migration.
 */
public enum Jurisdiction {
    EU,
    UK,
    EU_UK
}
