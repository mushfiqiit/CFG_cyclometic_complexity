package io.github.mushfiqiit.cfg;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A text table with a header row, a dashed separator and columns padded to line up. Add rows with
 * {@link #addRow(Object...)}, then call {@link #print(PrintStream)}.
 */
final class ConsoleTable {

    private static final String INDENT = "  ";
    private static final String COLUMN_GAP = "  ";

    private final List<String> headers;
    private final List<List<String>> rows = new ArrayList<>();
    private final Set<Integer> rightAligned = new HashSet<>();

    /**
     * Creates an empty table.
     *
     * @param headers the column headings, from left to right
     */
    ConsoleTable(String... headers) {
        this.headers = List.of(headers);
    }

    // ***************************************************************************
    // Filling the table
    // ***************************************************************************

    /**
     * Right-aligns columns, which suits numbers. Columns are left-aligned otherwise.
     *
     * @param columns zero-based indexes of the columns to right-align
     * @return this table, so calls can be chained
     */
    ConsoleTable alignRight(int... columns) {
        Arrays.stream(columns).forEach(rightAligned::add);
        return this;
    }

    /**
     * Adds a row. Each cell is converted with {@link String#valueOf(Object)}.
     *
     * @param cells one value per column
     * @return this table, so calls can be chained
     * @throws IllegalArgumentException if the number of cells differs from the number of columns
     */
    ConsoleTable addRow(Object... cells) {
        if (cells.length != headers.size()) {
            throw new IllegalArgumentException("expected " + headers.size() + " cells, got " + cells.length);
        }
        List<String> row = new ArrayList<>();
        for (Object cell : cells) {
            row.add(String.valueOf(cell));
        }
        rows.add(row);
        return this;
    }

    // ***************************************************************************
    // Printing the table
    // ***************************************************************************

    /**
     * Prints the header, a dashed separator and every row, indented by two spaces.
     *
     * @param out where to print
     */
    void print(PrintStream out) {
        int[] widths = columnWidths();
        printRow(out, headers, widths);
        List<String> separator = new ArrayList<>();
        for (int width : widths) {
            separator.add("-".repeat(width));
        }
        printRow(out, separator, widths);
        for (List<String> row : rows) {
            printRow(out, row, widths);
        }
    }

    /**
     * Finds the width of each column: its longest cell, including the heading.
     *
     * @return one width per column
     */
    private int[] columnWidths() {
        int[] widths = new int[headers.size()];
        for (int column = 0; column < widths.length; column++) {
            widths[column] = headers.get(column).length();
            for (List<String> row : rows) {
                widths[column] = Math.max(widths[column], row.get(column).length());
            }
        }
        return widths;
    }

    /**
     * Prints one row with each cell padded to its column's width, without trailing spaces.
     *
     * @param out where to print
     * @param cells the cells to print
     * @param widths the width of each column
     */
    private void printRow(PrintStream out, List<String> cells, int[] widths) {
        StringBuilder line = new StringBuilder(INDENT);
        for (int column = 0; column < cells.size(); column++) {
            if (column > 0) {
                line.append(COLUMN_GAP);
            }
            String format = "%" + (rightAligned.contains(column) ? "" : "-") + widths[column] + "s";
            line.append(String.format(format, cells.get(column)));
        }
        out.println(line.toString().stripTrailing());
    }
}
