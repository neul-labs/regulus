package com.regulus.platform.governance;

import java.time.Instant;
import java.util.List;

/**
 * ISO/IEC 42001 Statement of Applicability artefact.
 *
 * <p>Certification requires a documented SoA listing every Annex A control,
 * whether it is in scope (justified inclusion/exclusion), and how it is
 * implemented. Regulus generates this from the {@link GovernanceProgramState}
 * plus the {@link Iso42001Framework} control inventory.
 *
 * @param tenantId  the firm or business unit the SoA covers
 * @param framework the framework this SoA is being produced against (typically
 *                  {@code "iso-42001"})
 * @param effectiveFrom the date this SoA is valid from
 * @param rows      one row per control with status and justification
 */
public record StatementOfApplicability(
        String tenantId,
        String framework,
        Instant effectiveFrom,
        List<Row> rows) {

    public record Row(
            String controlId,
            String controlName,
            ControlImplementationStatus status,
            String justification,
            String evidenceRef) {
    }
}
