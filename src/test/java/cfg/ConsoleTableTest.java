package cfg;

import static cfg.TestSupport.captureOutput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Pins down table layout: widths, alignment, separators and trailing spaces. */
class ConsoleTableTest {

    @Test
    void padsColumnsToTheirWidestCellAndRightAlignsChosenColumns() {
        String output = captureOutput(() -> new ConsoleTable("Name", "N").alignRight(1)
                .addRow("a", 1)
                .addRow("long", 100)
                .print());

        assertEquals(String.join("\n",
                "  Name    N",
                "  ----  ---",
                "  a       1",
                "  long  100",
                ""), output);
    }

    @Test
    void doesNotLeaveTrailingSpaces() {
        String output = captureOutput(() -> new ConsoleTable("A", "B")
                .addRow("x", "longer")
                .addRow("y", "s")
                .print());

        assertEquals("  y  s", output.split("\n")[3]);
    }

    @Test
    void rejectsRowsWithTheWrongNumberOfCells() {
        ConsoleTable table = new ConsoleTable("A", "B");

        assertThrows(IllegalArgumentException.class, () -> table.addRow("only one"));
    }
}
