/**
 * ADK {@code BasePlugin} implementations that comprise the Regulus compliance
 * plane. Every plugin in this module hooks into ADK's official extension
 * surface ({@code BasePlugin} + the {@code Before*}/{@code After*} callback
 * family) rather than working around it.
 *
 * <p>The six plugins:
 * <ul>
 *   <li>{@link com.neullabs.regulus.adk.plugins.RegulusPolicyPlugin}
 *   <li>{@link com.neullabs.regulus.adk.plugins.RegulusPrivacyPlugin}
 *   <li>{@link com.neullabs.regulus.adk.plugins.RegulusAuditPlugin}
 *   <li>{@link com.neullabs.regulus.adk.plugins.RegulusKillSwitchPlugin}
 *   <li>{@link com.neullabs.regulus.adk.plugins.RegulusModelRiskPlugin}
 *   <li>{@link com.neullabs.regulus.adk.plugins.RegulusDataResidencyPlugin}
 * </ul>
 *
 * <p>Compose them into an ADK {@code App}:
 *
 * <pre>{@code
 * App app = App.builder("regulus-demo", rootAgent)
 *     .plugin(RegulusPolicyPlugin.fromProfile(profile))
 *     .plugin(RegulusPrivacyPlugin.withPatterns(NINO, IBAN, BIC, SORT_CODE).build())
 *     .plugin(RegulusKillSwitchPlugin.dualControl())
 *     .plugin(RegulusAuditPlugin.forProfile(profile).toKafka("audit.regulus.v1").build())
 *     .plugin(RegulusDataResidencyPlugin.allow("europe-west2"))
 *     .plugin(RegulusModelRiskPlugin.tier(Tier.STANDARD))
 *     .build();
 * }</pre>
 */
package com.neullabs.regulus.adk.plugins;
