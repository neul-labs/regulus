package com.neullabs.regulus.identity.oidc;

/**
 * Default OIDC claim names mapped into Regulus
 * {@link com.neullabs.regulus.identity.Claims}. Tenants can override the
 * mapping by supplying their own
 * {@link com.neullabs.regulus.identity.spi.IdentityAdapter} bean and
 * pre-empting the reference adapter via {@code @ConditionalOnMissingBean}.
 *
 * <p>The convention is:
 * <ul>
 *   <li>{@code sub} → {@code Principal.id}</li>
 *   <li>{@code preferred_username} / {@code name} → {@code Principal.displayName}</li>
 *   <li>{@code regulus.tenant} (fallback {@code tid}) → {@code Claims.tenantId}</li>
 *   <li>{@code regulus.jurisdiction} → {@code Claims.jurisdiction} (EU/UK/EU_UK)</li>
 *   <li>{@code regulus.purpose} (array) → {@code Claims.purposeCodes}</li>
 *   <li>{@code regulus.lawful_basis} (array) → {@code Claims.lawfulBases}</li>
 *   <li>{@code scope} (string, space-separated) ∪ {@code roles} (array)
 *       ∪ {@code realm_access.roles} → {@code Claims.roles}</li>
 * </ul>
 */
public final class OidcClaimNames {

    public static final String SUB = "sub";
    public static final String NAME = "name";
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String TENANT = "regulus.tenant";
    public static final String TENANT_FALLBACK = "tid";
    public static final String JURISDICTION = "regulus.jurisdiction";
    public static final String PURPOSE = "regulus.purpose";
    public static final String LAWFUL_BASIS = "regulus.lawful_basis";
    public static final String SCOPE = "scope";
    public static final String ROLES = "roles";
    public static final String REALM_ACCESS = "realm_access";

    private OidcClaimNames() {}
}
