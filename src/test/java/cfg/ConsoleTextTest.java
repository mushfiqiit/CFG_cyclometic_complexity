package cfg;

import static cfg.TestSupport.captureOutput;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Pins down headings, shortening and counted words. */
class ConsoleTextTest {

    @Test
    void titleIsUnderlinedWithEqualsSigns() {
        assertEquals("\nReport\n======\n", captureOutput(() -> ConsoleText.printTitle("Report")));
    }

    @Test
    void sectionIsUnderlinedWithDashes() {
        assertEquals("\nPart\n----\n", captureOutput(() -> ConsoleText.printSection("Part")));
    }

    @Test
    void shortenKeepsTextThatFitsAndCutsTextThatDoesNot() {
        assertEquals("abcdef", ConsoleText.shorten("abcdef", 6));
        assertEquals("abc...", ConsoleText.shorten("abcdefgh", 6));
    }

    @Test
    void countUsesTheSingularOnlyForOne() {
        assertEquals("1 loop", ConsoleText.count(1, "loop", "loops"));
        assertEquals("0 loops", ConsoleText.count(0, "loop", "loops"));
        assertEquals("2 loops", ConsoleText.count(2, "loop", "loops"));
    }
}
