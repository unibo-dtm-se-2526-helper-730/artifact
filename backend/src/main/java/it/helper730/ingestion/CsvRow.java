package it.helper730.ingestion;

import java.util.List;

/**
 * CsvRow.
 *
 * One record of a CSV file.
 *
 * @param line   line number where the record starts, starting from 1
 * @param fields values, unquoted
 */
public record CsvRow(int line, List<String> fields) {

    public CsvRow {
        fields = List.copyOf(fields);
    }

    /**
     * Whether all fields are empty or whitespace.
     */
    public boolean isBlank() {
        return fields.stream().allMatch(String::isBlank);
    }

    /**
     * Get the value of a field, or an empty string if the index is out of bounds.
     *
     * @param index index of the field
     * @return the value of the field, or an empty string if the index is out of
     *         bounds
     */
    public String field(int index) {
        return index < fields.size() ? fields.get(index).trim() : "";
    }
}
