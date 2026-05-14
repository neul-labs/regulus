package com.neullabs.regulus.grc.adapter;

import com.neullabs.regulus.grc.GrcEvidenceAdapter;
import com.neullabs.regulus.grc.GrcEvidenceEnvelope;

/** Development adapter — prints the envelope as JSON-ish stdout. */
public final class StdoutAdapter implements GrcEvidenceAdapter {

    @Override public String vendorId() { return "stdout"; }

    @Override public void emit(GrcEvidenceEnvelope envelope) {
        System.out.println("[regulus-grc] " + envelope);
    }
}
