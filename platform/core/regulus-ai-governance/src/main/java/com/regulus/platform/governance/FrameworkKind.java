package com.regulus.platform.governance;

public enum FrameworkKind {
    /** Best-practice framework, no certification path (e.g. NIST AI RMF). */
    VOLUNTARY,
    /** Standard with a certification scheme (e.g. ISO/IEC 42001). */
    CERTIFIABLE,
    /** Published technical / management-system standard (e.g. ISO/IEC 23894, 23053). */
    STANDARD
}
