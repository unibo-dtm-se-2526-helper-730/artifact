package it.helper730.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.helper730.ingestion.exceptions.MalformedCsvException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvReaderTest {

    private final CsvReader reader = new CsvReader();

    @Test
    void quotedFieldsMayContainSeparatorsAndEscapedQuotes() {
        List<CsvRow> rows = reader.read("a,\"b,c\",\"d \"\"e\"\"\"\n");

        assertThat(rows).singleElement().satisfies(r -> assertThat(r.fields()).containsExactly("a", "b,c", "d \"e\""));
    }

    @Test
    void recordsAreNumberedByTheLineWhereTheyStart() {
        List<CsvRow> rows = reader.read("h1,h2\r\n1,2\r\n\r\n3,4");

        assertThat(rows).extracting(CsvRow::line).containsExactly(1, 2, 3, 4);
        assertThat(rows.get(2).isBlank()).isTrue();
        assertThat(rows.get(3).fields()).containsExactly("3", "4");
    }

    @Test
    void quotedFieldMaySpanLines() {
        List<CsvRow> rows = reader.read("a,\"multi\nline\"\nb,c\n");

        assertThat(rows.get(0).field(1)).isEqualTo("multi\nline");
        assertThat(rows.get(1).line()).isEqualTo(3);
    }

    @Test
    void leadingByteOrderMarkIsIgnored() {
        assertThat(reader.read("\uFEFFDate,Ticker\n").get(0).fields()).containsExactly("Date", "Ticker");
    }

    @Test
    void rowOfSeparatorsIsBlank() {
        assertThat(reader.read(",,,,,,\n").get(0).isBlank()).isTrue();
    }

    @Test
    void unterminatedQuoteIsRejected() {
        assertThatThrownBy(() -> reader.read("a,\"open\n"))
                .isInstanceOfSatisfying(
                        MalformedCsvException.class, e -> assertThat(e.code()).isEqualTo("MALFORMED_CSV"));
    }

    @Test
    void emptyTextHasNoRecords() {
        assertThat(reader.read("")).isEmpty();
    }
}
