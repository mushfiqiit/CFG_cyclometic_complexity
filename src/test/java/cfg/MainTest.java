package cfg;

import static cfg.TestSupport.captureOutput;
import static cfg.TestSupport.programPath;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Runs the program end to end through {@link Main#main(String[])}. Error paths call
 * {@link System#exit(int)}, which would stop the test run, so they are not tested here.
 */
class MainTest {

    @Test
    void helpPrintsUsage() {
        String output = captureOutput(() -> Main.main(new String[] {"--help"}));

        assertTrue(output.startsWith("Usage: CFG_cyclometic_complexity [source-file]\n"));
        assertTrue(output.contains("-h, --help"));
    }

    @Test
    void optionTwoAnalyzesTheFileNamedOnTheCommandLine() {
        String path = programPath("sample.c").toString();

        String output = captureOutput("2\n", () -> Main.main(new String[] {path}));

        assertTrue(output.startsWith(String.join("\n",
                "Control Flow Graph Analyzer",
                "Source file: " + path + " (17 lines)",
                "",
                "Choose an analysis:",
                "  1. Cyclomatic complexity and control flow graph",
                "  2. Test case data (variables and conditions)",
                "Enter 1 or 2: ",
                "Test Case Data",
                "==============")), output);
        assertTrue(output.contains("  4    11  a>c        >         a, c\n"));
    }
}
