package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks the option 1 results, independently of how they are printed. */
class ComplexityAnalysisTest {

    @Test
    void sampleProgramResults() {
        ComplexityAnalysis analysis = ComplexityAnalysis.of(ParsedSource.of(program("sample.c")));

        assertEquals(16, analysis.graph().countNodes());
        assertEquals(19, analysis.graph().countEdges());
        assertEquals(1, analysis.graph().entry());
        assertEquals(new DecisionCounts(1, 2, 1), analysis.decisions());
        assertEquals(5, analysis.fromGraph());
        assertEquals(5, analysis.fromDecisions());
        assertTrue(analysis.resultsAgree());
    }

    @Test
    void resultsDisagreeWhenTheCountsDoNotMatchTheGraph() {
        ComplexityAnalysis sample = ComplexityAnalysis.of(ParsedSource.of(program("sample.c")));
        ComplexityAnalysis mismatched = new ComplexityAnalysis(sample.graph(), new DecisionCounts(1, 2, 2));

        assertEquals(6, mismatched.fromDecisions());
        assertFalse(mismatched.resultsAgree());
    }

    @Test
    void sourceWithoutCodeIsRejected() {
        ParsedSource empty = ParsedSource.of(List.of("#include <stdio.h>", ""));

        assertThrows(IllegalArgumentException.class, () -> ComplexityAnalysis.of(empty));
    }
}
