package it.helper730.ingestion;

/**
 * Severity.
 *
 * Indicates how serious a problem found in a row is.
 */
public enum Severity {
    /** The row is not imported. */
    ERROR,
    /** The row is imported, but the user should check it. */
    WARNING
}
