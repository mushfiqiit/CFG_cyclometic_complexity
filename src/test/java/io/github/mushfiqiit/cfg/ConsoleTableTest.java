package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.print;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Pins down table layout: widths, alignment, separators and trailing spaces. */
class ConsoleTableTest {

    @Test
    void padsColumnsToTheirWidestCellAndRightAlignsChosenColumns() {
        String output = print(out -> new ConsoleTable("Name", "N").alignRight(1)
                .addRow("a", 1)
                .addRow("long", 100)
                .print(out));

        assertEquals(String.join("\n",
                "  Name    N",
                "  ----  ---",
                "  a       1",
                "  long  100",
                ""), output);
    }

    @Test
    void doesNotLeaveTrailingSpaces() {
        String output = print(out -> new ConsoleTable("A", "B")
                .addRow("x", "longer")
                .addRow("y", "s")
                .print(out));

        assertEquals("  y  s", output.split("\n")[3]);
    }

    @Test
    void rejectsRowsWithTheWrongNumberOfCells() {
        ConsoleTable table = new ConsoleTable("A", "B");

        assertThrows(IllegalArgumentException.class, () -> table.addRow("only one"));
    }
}
