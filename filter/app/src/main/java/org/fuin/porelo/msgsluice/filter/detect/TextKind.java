package org.fuin.porelo.msgsluice.filter.detect;

/** What a text under check is written in, which decides the detectors that judge it. */
public enum TextKind {

    /** Words for a person to read: every detector judges it. */
    PROSE,

    /**
     * Source code, such as a suggested replacement in a review comment: judged by the encoded-data
     * checks with thresholds measured on code, and by the credential and personal-data checks, but
     * never by those that ask whether it reads like English.
     */
    CODE
}
