package it.helper730.service;

import java.time.Instant;

/**
 * ImportBatchSummary.
 *
 * One past import, as shown in the import history. Keeps the JPA entity inside
 * the service layer.
 *
 * @param id         id of the import batch
 * @param filename   name of the uploaded file
 * @param broker     broker recognised from the file
 * @param importedAt when the file was imported
 * @param imported   transactions saved
 * @param duplicates transactions skipped as duplicates
 * @param errors     rows rejected
 */
public record ImportBatchSummary(
        long id, String filename, String broker, Instant importedAt, int imported, int duplicates, int errors) {}
