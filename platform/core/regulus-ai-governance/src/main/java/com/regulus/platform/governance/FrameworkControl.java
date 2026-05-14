package com.regulus.platform.governance;

/**
 * One control or control-category from a {@link GovernanceFramework}.
 *
 * @param id          control identifier as the framework names it
 *                    (e.g. {@code "GOVERN-1.1"}, {@code "A.6.2.2"}).
 * @param category    high-level grouping (e.g. {@code "GOVERN"} for NIST,
 *                    {@code "A.6 AI System Lifecycle"} for ISO 42001).
 * @param name        short name from the framework.
 * @param description one-sentence summary in plain English.
 */
public record FrameworkControl(
        String id,
        String category,
        String name,
        String description) {
}
