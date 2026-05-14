package com.neullabs.regulus.governance;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatementOfApplicabilityTest {

    @Test
    void soaRowCarriesAllFields() {
        StatementOfApplicability soa = new StatementOfApplicability(
                "tenant-uk-bank-1",
                "iso-42001",
                Instant.parse("2026-05-14T00:00:00Z"),
                List.of(
                        new StatementOfApplicability.Row(
                                "A.6.2.7",
                                "Recording of event logs",
                                ControlImplementationStatus.IMPLEMENTED,
                                "RegulusAuditPlugin produces the AIMS-required event log.",
                                "audit-evidence://kafka/audit.regulus.v1"
                        ),
                        new StatementOfApplicability.Row(
                                "A.6.2.8",
                                "Change management",
                                ControlImplementationStatus.PARTIAL,
                                "RegulusKillSwitchPlugin covers immediate stops; "
                                        + "RFC-style change approval still off-Regulus.",
                                "runbook://operations/kill-switch-playbook"
                        )
                ));

        assertThat(soa.tenantId()).isEqualTo("tenant-uk-bank-1");
        assertThat(soa.framework()).isEqualTo("iso-42001");
        assertThat(soa.rows()).hasSize(2);
        assertThat(soa.rows().get(0).status()).isEqualTo(ControlImplementationStatus.IMPLEMENTED);
        assertThat(soa.rows().get(1).status()).isEqualTo(ControlImplementationStatus.PARTIAL);
    }
}
