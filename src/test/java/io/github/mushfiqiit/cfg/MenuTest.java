package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Checks the menu text, valid and invalid answers, and the end of the input. */
class MenuTest {

    private Optional<Menu.Analysis> choice;

    private String ask(String input) {
        return TestSupport.print(out -> choice = Menu.ask(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), out));
    }

    @Test
    void showsTheChoicesAndAcceptsTheFirstValidAnswer() {
        String output = ask("1\n");

        assertEquals(Optional.of(Menu.Analysis.COMPLEXITY), choice);
        assertEquals(String.join("\n",
                "",
                "Choose an analysis:",
                "  1. Cyclomatic complexity and control flow graph",
                "  2. Test case data (variables and conditions)",
                "Enter 1 or 2: "), output);
    }

    @Test
    void asksAgainAfterAnInvalidAnswer() {
        String output = ask("x\n 2 \n");

        assertEquals(Optional.of(Menu.Analysis.TEST_CASES), choice);
        assertEquals("Enter 1 or 2: Please enter 1 or 2.\nEnter 1 or 2: ", output.substring(output.indexOf("Enter")));
    }

    @Test
    void returnsNothingWhenTheInputEnds() {
        String output = ask("3\n");

        assertEquals(Optional.empty(), choice);
        assertEquals("Enter 1 or 2: Please enter 1 or 2.\nEnter 1 or 2: \n", output.substring(output.indexOf("Enter")));
    }
}
