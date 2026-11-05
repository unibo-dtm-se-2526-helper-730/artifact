package it.helper730.service;

import it.helper730.ingestion.RowIssue;
import java.util.List;

/**
 * ImportReport.
 *
 * Outcome of one import, before it is turned into the API response.
 *
 * @param batchId    id of the saved import batch
 * @param imported   transactions saved
 * @param duplicates transactions skipped because already stored or repeated in
 *                   the file
 * @param errors     rows rejected because of at least one ERROR
 * @param issues     errors and warnings, ordered by row
 */
public record ImportReport(long batchId, int imported, int duplicates, int errors, List<RowIssue> issues) {}
