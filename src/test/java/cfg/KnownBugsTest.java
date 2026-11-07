package cfg;

import static cfg.TestSupport.build;
import static cfg.TestSupport.captureOutput;
import static cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests that state the correct behavior for bugs that are not fixed yet, so they fail today.
 *
 * <p>They are tagged {@code known-bug}: {@code ./gradlew test} skips them and
 * {@code ./gradlew knownBugTest} runs them. When a bug is fixed, its test starts passing; remove
 * its tag then so it becomes part of the normal build.
 */
@Tag("known-bug")
class KnownBugsTest {

    // ***************************************************************************
    // Issue 1: blank lines crash option 1
    // ***************************************************************************

    @Test
    @DisplayName("Issue 1: a blank line after #include does not crash the graph builder")
    void blankLineInThePreambleDoesNotCrash() {
        List<String> typical = program("typical.c");

        ControlFlowGraphBuilder builder = assertDoesNotThrow(() -> ControlFlowGraphBuilder.from(typical));
        assertEquals("int main(void) {", builder.root().getStatement());
    }

    @Test
    @DisplayName("Issue 1: a blank line inside a function does not crash the graph builder")
    void blankLineInsideAFunctionDoesNotCrash() {
        List<String> lines = List.of(
                "\tint main(){",
                "    int a;",
                "",
                "    return 0;",
                "}");

        assertDoesNotThrow(() -> build(lines));
    }

    // ***************************************************************************
    // Issue 2: option 2 lists non-variables
    // ***************************************************************************

    @Test
    @DisplayName("Issue 2: option 2 lists only declared variable names")
    void testInputValuesListOnlyDeclaredVariables() {
        String output = captureOutput(() -> VariableConditionAnalysis.run(program("typical.c")));

        assertEquals(List.of("x", "y", "i"), variableNames(output));
    }

    /**
     * Reads the variable names from the "Test input values" table of an option 2 report.
     *
     * @param output the report
     * @return the first column of each table row
     */
    private static List<String> variableNames(String output) {
        List<String> names = new ArrayList<>();
        String[] lines = output.split("\n");
        int row = List.of(lines).indexOf("  Variable  Value");
        if (row < 0) {
            row = indexOfHeader(lines);
        }
        for (int i = row + 2; i < lines.length && !lines[i].isBlank(); i++) {
            names.add(lines[i].trim().split(" +")[0]);
        }
        return names;
    }

    /**
     * Finds the header row of the variables table, whose padding depends on the longest name.
     *
     * @param lines the report's lines
     * @return the header row's index
     */
    private static int indexOfHeader(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().matches("Variable +Value")) {
                return i;
            }
        }
        throw new AssertionError("no variables table in output");
    }

    // ***************************************************************************
    // Issue 3: N in the graph formula is not the node count
    // ***************************************************************************

    @Test
    @DisplayName("Issue 3: N in M = E - N + 2 is the number of nodes in the graph")
    void graphFormulaUsesTheNodeCount() {
        List<String> lines = program("two-preamble-lines.c");
        ControlFlowGraph graph = build(lines);

        String output = captureOutput(() ->
                GraphPrinter.printComplexity(graph, ComplexityCalculator.countDecisions(lines)));

        // Four nodes (lines 3 to 6) and three edges between them.
        assertTrue(output.contains("M = E - N + 2 = 3 - 4 + 2 = 1"), output);
    }
}
