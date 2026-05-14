package com.neullabs.regulus.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegulusCliTest {

    @Test
    void versionIsBundled() {
        assertThat(RegulusCli.VERSION).isNotBlank().contains(".");
    }
}
