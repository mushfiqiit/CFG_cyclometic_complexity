package cfg;

import static cfg.TestSupport.build;
import static cfg.TestSupport.captureOutput;
import static cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pins down the option 1 report for the sample program. */
class GraphPrinterTest {

    private final List<String> sample = program("sample.c");
    private final ControlFlowGraph graph = build(sample);

    /** Collapses runs of spaces, so a row can be checked without depending on column widths. */
    private static List<String> collapsedLines(String output) {
        return Arrays.stream(output.split("\n")).map(line -> line.trim().replaceAll(" +", " ")).toList();
    }

    @Test
    void entryPointShowsTheOneBasedLineAndStatement() {
        String output = captureOutput(() ->
                GraphPrinter.printEntryPoint(ControlFlowGraphBuilder.from(sample).root()));

        assertEquals("Entry point: line 2: int main(){\n", output);
    }

    @Test
    void successorTableListsEachReachableLineWithItsNextLines() {
        String output = captureOutput(() -> GraphPrinter.printGraph(graph));
        String table = output.substring(output.indexOf("  Line  Statement"), output.indexOf("\n\nAdjacency matrix"));

        assertEquals(String.join("\n",
                "  Line  Statement                       Next lines",
                "  ----  ------------------------------  ------------",
                "     2  int main(){                     3",
                "     3  int a, b, c, k;                 4",
                "     4  while(k>0){                     5, 8, 11, 16",
                "     5  if(a>b){                        6",
                "     6  printf(\"a is bigger than b\");   7",
                "     7  }                               14",
                "     8  else if(a==b){                  9",
                "     9  printf(\"a and b is equal\");     10",
                "    10  }                               14",
                "    11  else if(a>c) {                  12",
                "    12  printf(\"a is smaller than b\");  13",
                "    13  }                               14",
                "    14  k--;                            15",
                "    15  }                               4",
                "    16  return 0;                       17",
                "    17  }                               (end)"), table);
    }

    @Test
    void adjacencyMatrixHasLineNumberHeadersAndMarksEdgesWithOne() {
        List<String> lines = collapsedLines(captureOutput(() -> GraphPrinter.printGraph(graph)));

        assertTrue(lines.contains("Line 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17"));
        assertTrue(lines.contains("4 . . . 1 . . 1 . . 1 . . . . 1 ."));
        assertTrue(lines.contains("15 . . 1 . . . . . . . . . . . . ."));
    }

    @Test
    void complexityShowsBothFormulasAndTheDecisionPoints() {
        String output = captureOutput(() ->
                GraphPrinter.printComplexity(graph, ComplexityCalculator.countDecisions(sample)));

        assertEquals(String.join("\n",
                "",
                "Cyclomatic complexity",
                "---------------------",
                "  From the graph:            M = E - N + 2 = 18 - 16 + 2 = 4",
                "  From the decision points:  M = D + 1 = 4 + 1 = 5",
                "  Decision points: 1 if, 0 else ifs, 2 elses, 1 loop",
                ""), output);
    }
}
