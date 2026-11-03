package it.helper730.ingestion;

import it.helper730.domain.Msg;
import it.helper730.ingestion.exceptions.MalformedCsvException;
import it.helper730.ingestion.exceptions.UnsupportedBrokerException;
import java.util.List;
import java.util.Objects;

/**
 * BrokerParserRegistry.
 *
 * Finds the parser of a file from its header and runs it.
 */
public final class BrokerParserRegistry {

    private final CsvReader csvReader;
    private final List<BrokerParser> parsers;

    public BrokerParserRegistry(CsvReader csvReader, List<BrokerParser> parsers) {
        this.csvReader = Objects.requireNonNull(csvReader, "csvReader");
        this.parsers = List.copyOf(parsers);
    }

    /** Parse CSV text. */
    public ParseResult parse(String content) {
        return parse(csvReader.read(content));
    }

    /** Parse a list of CSV rows. */
    private ParseResult parse(List<CsvRow> rows) {
        if (rows.isEmpty()) {
            throw new MalformedCsvException(Msg.of("error.empty_file"));
        }
        CsvRow header = rows.getFirst();
        BrokerParser parser = parsers.stream()
                .filter(p -> p.supports(header))
                .findFirst()
                .orElseThrow(() -> new UnsupportedBrokerException(String.join(",", header.fields())));
        return parser.parse(rows.subList(1, rows.size()));
    }
}
