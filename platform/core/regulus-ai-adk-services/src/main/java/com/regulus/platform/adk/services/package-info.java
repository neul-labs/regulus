/**
 * Service-level extensions on top of ADK's official
 * {@code SessionService} / {@code MemoryService} / {@code ArtifactService} /
 * {@code EventCompactor} / {@code BaseComputer} interfaces.
 *
 * <p>Each class in this module wraps the Google-shipped implementation,
 * adding fail-closed residency validation, CMEK enforcement, retention
 * windows, and an audit envelope — without changing the API surface ADK
 * callers expect.
 */
package com.regulus.platform.adk.services;
