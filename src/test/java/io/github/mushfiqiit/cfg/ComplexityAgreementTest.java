package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Checks the two complexity calculations against each other on many generated programs, and that
 * the format checker accepts every one of them. For a
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
            assertEquals(List.of(), SourceFormatChecker.check(ParsedSource.of(CommentStripper.strip(lines))),
                    () -> "a supported program was rejected:\n" + String.join("\n", lines));
            ControlFlowGraph graph = TestSupport.build(lines);
            int fromGraph = graph.countEdges() - graph.countNodes() + 2;
            int fromDecisions = ComplexityCalculator.countDecisions(ParsedSource.of(lines)).complexity();

            assertEquals(fromDecisions, fromGraph, () -> "complexities differ for:\n" + String.join("\n", lines));
        }
    }

    @Test
    void generatedTestCasesReachTheirConditionWithTheRightOutcome() {
        Random random = new Random(20261108);
        int withValues = 0;
        int contradictions = 0;
        for (int program = 0; program < PROGRAMS; program++) {
            List<String> lines = new ArrayList<>(List.of("int main(void) {", "    int a, b, k;"));
            addStatements(lines, random, 1);
            lines.add("}");
            Map<String, DeclaredVariable> declared = new LinkedHashMap<>();
            List.of("a", "b", "k").forEach(name -> declared.put(name, DeclaredVariable.input(name)));

            for (TestCaseGenerator.TestCase testCase : TestCaseGenerator.generate(ConditionPaths.find(ParsedSource.of(lines)), declared)) {
                List<ConditionSite.Requirement> required = new ArrayList<>(testCase.site().path());
                required.add(new ConditionSite.Requirement(testCase.site().lineNumber(), testCase.site().condition(),
                        testCase.outcome()));
                if (testCase.inputs().isPresent()) {
                    withValues++;
                    assertTrue(allHold(required, testCase.inputs().orElseThrow()),
                            () -> "values do not reach " + testCase.site().describe() + " in:\n" + String.join("\n", lines));
                } else {
                    contradictions++;
                    assertEquals(new TestCaseGenerator.NoValues(new TestCaseGenerator.Contradiction()), testCase.result());
                    assertTrue(noValuesInRangeWork(required),
                            () -> "a reachable outcome was reported as impossible: " + testCase.site().describe()
                                    + " " + testCase.outcome() + " in:\n" + String.join("\n", lines));
                }
            }
        }
        assertTrue(withValues > 1000, "too few test cases were checked: " + withValues);
        assertTrue(contradictions > 0, "no impossible outcomes were checked");
    }

    /** Tries every value from -8 to 8 for a, b and k; the generated constants are all between 0 and 4. */
    private static boolean noValuesInRangeWork(List<ConditionSite.Requirement> required) {
        Map<String, Integer> values = new HashMap<>();
        for (int a = -8; a <= 8; a++) {
            for (int b = -8; b <= 8; b++) {
                for (int k = -8; k <= 8; k++) {
                    values.put("a", a);
                    values.put("b", b);
                    values.put("k", k);
                    if (allHold(required, values)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Evaluates each required comparison independently of the generator. */
    private static boolean allHold(List<ConditionSite.Requirement> required, Map<String, Integer> values) {
        for (ConditionSite.Requirement step : required) {
            Condition.Compare comparison = (Condition.Compare) step.condition();
            int left = value(comparison.left(), values);
            int right = value(comparison.right(), values);
            boolean holds = switch (comparison.sign()) {
                case ">" -> left > right;
                case "<" -> left < right;
                default -> throw new AssertionError("unexpected operator in " + comparison.text());
            };
            if (holds != step.outcome()) {
                return false;
            }
        }
        return true;
    }

    private static int value(String operand, Map<String, Integer> values) {
        return operand.matches("-?\\d+") ? Integer.parseInt(operand) : values.get(operand);
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
                lines.add(indent + List.of("a = 1;", "b++;", "printf(\"x\");", "return 0;", "// note",
                        "c = 2; /* inline */", "int v[2] = {1, 2};").get(random.nextInt(7)));
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
