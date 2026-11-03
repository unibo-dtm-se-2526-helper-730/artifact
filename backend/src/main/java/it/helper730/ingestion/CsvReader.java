package it.helper730.ingestion;

import it.helper730.domain.Msg;
import it.helper730.ingestion.exceptions.MalformedCsvException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CsvReader.
 *
 * Reads CSV with comma separator, fields optionally enclosed
 * in double quotes, a double quote inside a quoted field written as two, line
 * breaks allowed inside quotes. Accepts both CRLF and LF line endings and
 * ignores a leading UTF-8 byte order mark.
 */
public final class CsvReader {

    private static final char QUOTE = '"';
    private static final char SEPARATOR = ',';
    private static final char BOM = '\uFEFF';

    /** Reads a UTF-8 stream. */
    public List<CsvRow> read(InputStream input) {
        try {
            return read(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads CSV text into records, numbered with the line where each starts.
     *
     * @throws MalformedCsvException if a quoted field is never closed
     */
    public List<CsvRow> read(String content) {
        String text = !content.isEmpty() && content.charAt(0) == BOM ? content.substring(1) : content;
        List<CsvRow> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean rowStarted = false;
        int line = 1;
        int rowStart = 1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == QUOTE) {
                    if (i + 1 < text.length() && text.charAt(i + 1) == QUOTE) {
                        field.append(QUOTE);
                    } else {
                        inQuotes = false;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append(c);
                }
                continue;
            }
            switch (c) {
                case QUOTE -> {
                    if (field.isEmpty()) {
                        inQuotes = true;
                    } else {
                        field.append(c);
                    }
                    rowStarted = true;
                }
                case SEPARATOR -> {
                    fields.add(field.toString());
                    field.setLength(0);
                    rowStarted = true;
                }
                case '\r' -> {
                    // part of a CRLF line ending: the \n that follows ends the record
                }
                case '\n' -> {
                    fields.add(field.toString());
                    rows.add(new CsvRow(rowStart, fields));
                    fields = new ArrayList<>();
                    field.setLength(0);
                    rowStarted = false;
                    line++;
                    rowStart = line;
                }
                default -> {
                    field.append(c);
                    rowStarted = true;
                }
            }
        }
        if (inQuotes) {
            throw new MalformedCsvException(Msg.of("error.csv_unterminated_quote", rowStart));
        }
        if (rowStarted) {
            fields.add(field.toString());
            rows.add(new CsvRow(rowStart, fields));
        }
        return List.copyOf(rows);
    }
}
