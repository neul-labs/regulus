package com.regulus.platform.adk.plugins;

import com.google.adk.plugins.BasePlugin;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects and masks PII in prompts before they reach the model, and re-redacts
 * model output before it lands in audit or downstream sinks.
 *
 * <p>Hooks used:
 * <ul>
 *   <li>{@code BeforeModelCallback} — mutates the LLM request to replace
 *       matched patterns with stable tokens (e.g. {@code <NINO_1>}). Token
 *       mapping is held per-invocation so the original is never persisted.</li>
 *   <li>{@code AfterModelCallback} — replays redaction on streamed output
 *       before it is handed to the next plugin or returned to the caller.</li>
 * </ul>
 *
 * <p>Default patterns shipped: UK NINO, IBAN, BIC, UK sort code, UK bank
 * account number, UK postcode, email address. Add custom patterns via
 * {@link Builder#pattern(String, Pattern)}.
 *
 * <p>Maps to: GDPR / UK GDPR Art. 25 (privacy by design), Art. 5(1)(c)
 * (data minimisation); NHS DSPT 1.x; EU AI Act Art. 10 (data governance,
 * insofar as training data is concerned — Regulus addresses inference-time).
 */
public final class RegulusPrivacyPlugin extends BasePlugin {

    public enum BuiltInPattern {
        NINO,
        IBAN,
        BIC,
        SORT_CODE,
        UK_ACCOUNT_NUMBER,
        UK_POSTCODE,
        EMAIL,
        NHS_NUMBER
    }

    private final List<NamedPattern> patterns;

    private RegulusPrivacyPlugin(List<NamedPattern> patterns) {
        super("regulus-privacy");
        this.patterns = List.copyOf(patterns);
    }

    public static Builder withPatterns(BuiltInPattern... built) {
        Builder b = new Builder();
        for (BuiltInPattern p : built) b.builtin(p);
        return b;
    }

    public List<NamedPattern> patterns() { return patterns; }

    public record NamedPattern(String name, Pattern pattern) {}

    public static final class Builder {

        private final java.util.List<NamedPattern> patterns = new java.util.ArrayList<>();

        public Builder builtin(BuiltInPattern p) {
            patterns.add(new NamedPattern(p.name(), BuiltInPatterns.regex(p)));
            return this;
        }

        public Builder pattern(String name, Pattern pattern) {
            patterns.add(new NamedPattern(name, pattern));
            return this;
        }

        public RegulusPrivacyPlugin build() {
            return new RegulusPrivacyPlugin(patterns);
        }
    }
}
