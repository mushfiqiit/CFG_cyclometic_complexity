package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Checks the two complexity calculations against each other on many generated programs. For a
 * well-structured function, the graph gives E - N + 2 and the decision points give D + 1, and the
 * two must be equal; a difference means the graph has a missing or extra edge.
 */
class ComplexityAgreementTest {

    private static final int PROGRAMS = 500;

    @Test
    void graphAndDecisionPointsGiveTheSameComplexity() {
        Random random = new Random(20261107);
        for (int checked = 0; checked < PROGRAMS; checked++) {
            List<String> lines = new ArrayList<>(List.of("#include<stdio.h>", "", "int main(void) {"));
            addStatements(lines, random, 1);
            lines.add("}");
            ControlFlowGraph graph = TestSupport.build(lines);
            int fromGraph = graph.countEdges() - graph.countNodes() + 2;
            int fromDecisions = ComplexityCalculator.countDecisions(lines).complexity();

            assertEquals(fromDecisions, fromGraph, () -> "complexities differ for:\n" + String.join("\n", lines));
        }
    }

    /**
     * Appends one to three random statements: plain lines, blank lines, if chains and loops, with
     * nesting up to three levels deep.
     */
    private static void addStatements(List<String> lines, Random random, int depth) {
        String indent = "    ".repeat(depth);
        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            int kind = depth > 3 ? 0 : random.nextInt(10);
            if (kind < 4) {
                lines.add(indent + List.of("a = 1;", "b++;", "printf(\"x\");", "return 0;").get(random.nextInt(4)));
            } else if (kind < 5) {
                lines.add("");
            } else if (kind < 8) {
                lines.add(indent + "if (a > " + depth + ") {");
                addBody(lines, random, depth);
                for (int branch = random.nextInt(3); branch > 0; branch--) {
                    lines.add(indent + "else if (b < " + branch + ") {");
                    addBody(lines, random, depth);
                }
                if (random.nextBoolean()) {
                    lines.add(indent + "else {");
                    addBody(lines, random, depth);
                }
            } else {
                lines.add(indent + (random.nextBoolean() ? "while (k > 0) {" : "for (int i = 0; i < 3; i++) {"));
                addBody(lines, random, depth);
            }
        }
    }

    /** Appends a block body, which is sometimes empty, and its closing brace. */
    private static void addBody(List<String> lines, Random random, int depth) {
        if (random.nextInt(5) > 0) {
            addStatements(lines, random, depth + 1);
        }
        lines.add("    ".repeat(depth) + "}");
    }
}
