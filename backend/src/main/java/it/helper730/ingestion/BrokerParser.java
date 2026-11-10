package it.helper730.ingestion;

import java.util.List;

/**
 * BrokerParser.
 *
 * Reads the export of one broker. One implementation per supported format.
 */
public interface BrokerParser {

    /** Name of the broker, stored with each import. */
    String broker();

    /**
     * Country where the broker holds the securities, written in the quadro W of the
     * declaration.
     */
    String country();

    /** Whether this parser recognizes the header row of a file. */
    boolean supports(CsvRow header);

    /**
     * Parses the records after the header.
     *
     * @param rows records of the file, header excluded
     */
    ParseResult parse(List<CsvRow> rows);
}
