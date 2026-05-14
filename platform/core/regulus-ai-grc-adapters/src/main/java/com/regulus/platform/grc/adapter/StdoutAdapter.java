package com.regulus.platform.grc.adapter;

import com.regulus.platform.grc.GrcEvidenceAdapter;
import com.regulus.platform.grc.GrcEvidenceEnvelope;

/** Development adapter — prints the envelope as JSON-ish stdout. */
public final class StdoutAdapter implements GrcEvidenceAdapter {

    @Override public String vendorId() { return "stdout"; }

    @Override public void emit(GrcEvidenceEnvelope envelope) {
        System.out.println("[regulus-grc] " + envelope);
    }
}
