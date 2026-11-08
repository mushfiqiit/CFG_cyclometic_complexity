package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.print;
import static io.github.mushfiqiit.cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Pins down the option 2 report. */
class VariableConditionAnalysisTest {

    @Test
    void sampleProgramReport() {
        String output = print(out -> VariableConditionAnalysis.run(program("sample.c"), out));

        assertEquals(String.join("\n",
                "",
                "Test Case Data",
                "==============",
                "",
                "Test input values",
                "-----------------",
                "  Variable  Value",
                "  --------  -----",
                "  a             0",
                "  b             0",
                "  c             0",
                "  k             0",
                "",
                "Conditions to cover",
                "-------------------",
                "  #  Line  Condition  Operator  Operands",
                "  -  ----  ---------  --------  --------",
                "  1     4  k>0        >         k, 0",
                "  2     5  a>b        >         a, b",
                "  3     8  a==b       ==        a, b",
                "  4    11  a>c        >         a, c",
                ""), output);
    }

    @Test
    void saysSoWhenThereAreNoVariablesOrConditions() {
        String output = print(out -> VariableConditionAnalysis.run(List.of("\tint main(){", "}"), out));

        assertTrue(output.contains("  No int variables were declared."));
        assertTrue(output.contains("  No if, else if or while conditions were found."));
    }

    @Test
    void emptyConditionIsShownWithDashes() {
        String output = print(out -> VariableConditionAnalysis.run(List.of("if(){", "}"), out));

        assertTrue(output.contains("  1     1  -          -         -"));
    }

    @Test
    void declarationEndingInACommaAddsNoBlankVariable() {
        String output = print(out -> VariableConditionAnalysis.run(List.of("  int y, "), out));

        assertTrue(output.contains("  y             0\n\n"));
    }

    /** Reads the variable names from the "Test input values" table of an option 2 report. */
    private static List<String> variableNames(String output) {
        List<String> lines = List.of(output.split("\n"));
        int header = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().matches("Variable +Value")) {
                header = i;
            }
        }
        List<String> names = new java.util.ArrayList<>();
        for (int i = header + 2; header >= 0 && i < lines.size() && !lines.get(i).isBlank(); i++) {
            names.add(lines.get(i).trim().split(" +")[0]);
        }
        return names;
    }

    @Test
    void typicalProgramListsOnlyDeclaredVariablesIncludingForLoopCounters() {
        String output = print(out -> VariableConditionAnalysis.run(program("typical.c"), out));

        assertEquals(List.of("x", "y", "i"), variableNames(output));
    }

    @Test
    void namesAreReadWithoutInitialValuesPointersOrArraySizes() {
        String output = print(out -> VariableConditionAnalysis.run(List.of(
                "    int a,b ,c , d;",
                "\tint tabbed;",
                "    int *p = &a, arr[3] = {1, 2, 3}, n = f(1, 2);",
                "integer z;",
                "int f(int x);",
                "int;"), out));

        assertEquals(List.of("a", "b", "c", "d", "tabbed", "p", "arr", "n"), variableNames(output));
    }
}
