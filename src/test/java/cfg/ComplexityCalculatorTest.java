package cfg;

import static cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Pins down how decision points are counted. */
class ComplexityCalculatorTest {

    /**
     * Known bug, pinned as current behavior: {@code else if} lines are counted as {@code else}, so
     * the sample's two {@code else if} lines appear as elses. Update this test when the bug is
     * fixed.
     */
    @Test
    void sampleProgramCountsElseIfAsElse() {
        DecisionCounts counts = ComplexityCalculator.countDecisions(program("sample.c"));

        assertEquals(new DecisionCounts(1, 0, 2, 1), counts);
        assertEquals(4, counts.total());
        assertEquals(5, counts.complexity());
    }

    @Test
    void forAndWhileAreBothLoops() {
        DecisionCounts counts = ComplexityCalculator.countDecisions(List.of(
                "for(i=0;i<3;i++){", "while (a != b) {", "do {"));

        assertEquals(new DecisionCounts(0, 0, 0, 2), counts);
    }

    @Test
    void programWithoutBranchesHasComplexityOne() {
        assertEquals(1, ComplexityCalculator.countDecisions(List.of("int main(){", "return 0;", "}"))
                .complexity());
    }
}
