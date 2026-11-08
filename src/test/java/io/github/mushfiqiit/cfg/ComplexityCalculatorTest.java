package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Pins down how decision points are counted. */
class ComplexityCalculatorTest {

    @Test
    void sampleProgramCountsEachKindOfDecision() {
        DecisionCounts counts = ComplexityCalculator.countDecisions(program("sample.c"));

        assertEquals(new DecisionCounts(1, 2, 1), counts);
        assertEquals(4, counts.total());
        assertEquals(5, counts.complexity());
    }

    @Test
    void forAndWhileAreBothLoops() {
        DecisionCounts counts = ComplexityCalculator.countDecisions(List.of(
                "for(i=0;i<3;i++){", "while (a != b) {", "do {"));

        assertEquals(new DecisionCounts(0, 0, 2), counts);
    }

    @Test
    void plainElseIsNotADecisionPoint() {
        DecisionCounts counts = ComplexityCalculator.countDecisions(List.of(
                "if(a){", "}", "else{", "}", "elsewhere=1;"));

        assertEquals(new DecisionCounts(1, 0, 0), counts);
        assertEquals(2, counts.complexity());
    }

    @Test
    void programWithoutBranchesHasComplexityOne() {
        assertEquals(1, ComplexityCalculator.countDecisions(List.of("int main(){", "return 0;", "}"))
                .complexity());
    }
}
