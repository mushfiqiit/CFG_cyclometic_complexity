package cfg;

import static cfg.TestSupport.captureOutput;
import static cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Pins down the option 2 report. */
class VariableConditionAnalysisTest {

    @Test
    void sampleProgramReport() {
        String output = captureOutput(() -> VariableConditionAnalysis.run(program("sample.c")));

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
        String output = captureOutput(() -> VariableConditionAnalysis.run(List.of("\tint main(){", "}")));

        assertTrue(output.contains("  No int variables were declared."));
        assertTrue(output.contains("  No if, else if or while conditions were found."));
    }

    @Test
    void emptyConditionIsShownWithDashes() {
        String output = captureOutput(() -> VariableConditionAnalysis.run(List.of("if(){", "}")));

        assertTrue(output.contains("  1     1  -          -         -"));
    }

    @Test
    void declarationEndingInACommaAddsNoBlankVariable() {
        String output = captureOutput(() -> VariableConditionAnalysis.run(List.of("  int y, ")));

        assertTrue(output.contains("  y             0\n\n"));
    }
}
