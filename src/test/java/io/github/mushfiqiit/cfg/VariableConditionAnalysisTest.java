package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.print;
import static io.github.mushfiqiit.cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Pins down the option 2 report. */
class VariableConditionAnalysisTest {

    /** Analyzes a source file and returns the printed option 2 report. */
    private static String report(ParsedSource source) {
        return print(out -> new TestCaseReport(out).print(VariableConditionAnalysis.of(source)));
    }

    @Test
    void sampleProgramReport() {
        String output = report(ParsedSource.of(program("sample.c")));

        assertEquals(String.join("\n",
                "",
                "Test Case Data",
                "==============",
                "",
                "Variables",
                "---------",
                "  Variable  Value",
                "  --------  -----",
                "  a         input",
                "  b         input",
                "  c         input",
                "  k         input",
                "",
                "Conditions to cover",
                "-------------------",
                "  #  Line  Condition  Operator  Operands",
                "  -  ----  ---------  --------  --------",
                "  1     4  k>0        >         k, 0",
                "  2     5  a>b        >         a, b",
                "  3     8  a==b       ==        a, b",
                "  4    11  a>c        >         a, c",
                "",
                "Test cases",
                "----------",
                "  Each condition is made true in one test case and false in another, using inputs",
                "  that reach it. Inputs are the values at the start of the function; assignments",
                "  made before a condition is checked are not taken into account.",
                "",
                "  #  Covers        Outcome  Inputs",
                "  -  ------------  -------  -------------------",
                "  1  line 4: k>0   true     a=0, b=0, c=0, k=1",
                "  2  line 4: k>0   false    a=0, b=0, c=0, k=0",
                "  3  line 5: a>b   true     a=1, b=0, c=0, k=1",
                "  4  line 5: a>b   false    a=0, b=0, c=0, k=1",
                "  5  line 8: a==b  true     a=0, b=0, c=0, k=1",
                "  6  line 8: a==b  false    a=0, b=1, c=0, k=1",
                "  7  line 11: a>c  true     a=0, b=1, c=-1, k=1",
                "  8  line 11: a>c  false    a=0, b=1, c=0, k=1",
                "",
                "  Values found for 8 of 8 condition outcomes.",
                ""), output);
    }

    @Test
    void saysSoWhenThereAreNoVariablesOrConditions() {
        String output = report(ParsedSource.of(List.of("\tint main(){", "}")));

        assertTrue(output.contains("  No int variables were declared."));
        assertTrue(output.contains("  No if, else if or while conditions were found."));
        assertTrue(output.contains("  No test cases: there are no conditions to cover."));
    }

    @Test
    void emptyConditionIsShownWithDashes() {
        String output = report(ParsedSource.of(List.of("if(){", "}")));

        assertTrue(output.contains("  1     1  -          -         -"));
        assertTrue(output.contains("no values: the condition is too complex to solve"));
    }

    @Test
    void declarationEndingInACommaAddsNoBlankVariable() {
        String output = report(ParsedSource.of(List.of("  int y, ")));

        assertTrue(output.contains("  y         input\n\n"));
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
        String output = report(ParsedSource.of(program("typical.c")));

        assertEquals(List.of("x", "y", "i"), variableNames(output));
    }

    @Test
    void namesAreReadWithoutInitialValuesPointersOrArraySizes() {
        String output = report(ParsedSource.of(List.of(
                "    int a,b ,c , d;",
                "\tint tabbed;",
                "    int *p = &a, arr[3] = {1, 2, 3}, n = f(1, 2);",
                "integer z;",
                "int f(int x);",
                "int;")));

        assertEquals(List.of("a", "b", "c", "d", "tabbed", "p", "arr", "n"), variableNames(output));
    }

    @Test
    void variablesShowWhereTheirValuesComeFrom() {
        String output = report(ParsedSource.of(List.of(
                "int main(void) {",
                "    int a, limit = 10, n = f(1);",
                "    for (int i = 0; i < 3; i++) {",
                "    }",
                "    if (z > 0) {",
                "    }",
                "}")));

        assertTrue(output.contains(String.join("\n",
                "  a         input",
                "  limit     10 (set when declared)",
                "  n         computed in the code",
                "  i         computed in the code",
                "  z         input (used but not declared)")), output);
    }
}
