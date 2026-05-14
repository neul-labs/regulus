package com.neullabs.regulus.grc.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared ObjectMapper config for GRC adapters. Centralised here so every
 * vendor adapter serialises {@link java.time.Instant} the same way
 * (ISO-8601 string, not epoch millis).
 */
final class AdapterJson {

    private AdapterJson() {}

    static ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
