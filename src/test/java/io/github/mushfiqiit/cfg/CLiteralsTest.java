package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Checks how string and character literals are found and blanked out. */
class CLiteralsTest {

    @Test
    void recognizesBothKindsOfQuote() {
        assertTrue(CLiterals.isQuote('"'));
        assertTrue(CLiterals.isQuote('\''));
        assertFalse(CLiterals.isQuote('`'));
    }

    @Test
    void findsTheEndOfALiteralSkippingEscapes() {
        String line = "printf(\"say \\\"hi\\\"\"); x = '}';";

        int start = line.indexOf('"');
        assertEquals(line.indexOf(");"), CLiterals.endOf(line, start));
        assertEquals(line.length() - 1, CLiterals.endOf(line, line.indexOf('\'')));
    }

    @Test
    void literalThatIsNotClosedRunsToTheEndOfTheLine() {
        assertEquals(9, CLiterals.endOf("x = \"open", 4));
    }

    @Test
    void blankingOutKeepsTheCodeAndItsPositions() {
        String literal = "\"{ and } // here\"";

        assertEquals("printf(" + " ".repeat(literal.length()) + ");  c = " + " ".repeat(3) + ";",
                CLiterals.blankOut("  printf(" + literal + ");  c = '{';  "));
    }
}
