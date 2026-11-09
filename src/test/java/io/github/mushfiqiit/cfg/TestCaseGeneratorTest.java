package io.github.mushfiqiit.cfg;

import static io.github.mushfiqiit.cfg.TestSupport.program;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mushfiqiit.cfg.TestCaseGenerator.TestCase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Checks the values generated for each condition outcome, and the reasons when there are none. */
class TestCaseGeneratorTest {

    private static List<TestCase> generate(Map<String, DeclaredVariable> declared, String... lines) {
        return TestCaseGenerator.generate(ConditionPaths.find(ParsedSource.of(List.of(lines))), declared);
    }

    private static Map<String, DeclaredVariable> inputs(String... names) {
        Map<String, DeclaredVariable> declared = new LinkedHashMap<>();
        for (String name : names) {
            declared.put(name, DeclaredVariable.input(name));
        }
        return declared;
    }

    /** Describes a test case as "true: a=1, b=0" or "false: no values: reason", using the report's wording. */
    private static String describe(TestCase testCase) {
        return testCase.outcome() + ": " + TestCaseReport.describe(testCase.result());
    }

    private static List<String> describeAll(List<TestCase> cases) {
        return cases.stream().map(TestCaseGeneratorTest::describe).toList();
    }

    @Test
    void sampleProgramGetsReachingValuesForEveryOutcome() {
        List<TestCase> cases = TestCaseGenerator.generate(ConditionPaths.find(ParsedSource.of(program("sample.c"))), inputs("a", "b", "c", "k"));

        assertEquals(List.of(
                "true: a=0, b=0, c=0, k=1",
                "false: a=0, b=0, c=0, k=0",
                "true: a=1, b=0, c=0, k=1",
                "false: a=0, b=0, c=0, k=1",
                "true: a=0, b=0, c=0, k=1",
                "false: a=0, b=1, c=0, k=1",
                "true: a=0, b=1, c=-1, k=1",
                "false: a=0, b=1, c=0, k=1"), describeAll(cases));
    }

    @Test
    void solvesEveryOperatorAndLiteralsOnEitherSide() {
        List<TestCase> cases = generate(inputs("a"),
                "int main(void) {",
                "    if (a < 3) {", "    }",
                "    if (a <= -2) {", "    }",
                "    if (a >= 7) {", "    }",
                "    if (a != 4) {", "    }",
                "    if (5 == a) {", "    }",
                "    if (a) {", "    }",
                "    if (!a) {", "    }",
                "}");

        assertEquals(List.of(
                "true: a=0", "false: a=3",
                "true: a=-2", "false: a=0",
                "true: a=7", "false: a=0",
                "true: a=0", "false: a=4",
                "true: a=5", "false: a=0",
                "true: a=1", "false: a=0",
                "true: a=0", "false: a=1"), describeAll(cases));
    }

    @Test
    void fixedVariablesKeepTheirDeclaredValue() {
        Map<String, DeclaredVariable> declared = inputs("y");
        declared.put("x", DeclaredVariable.fixed("x", 5));

        List<TestCase> cases = generate(declared, "int main(void) {", "    if (x > y) {", "    }", "}");

        assertEquals(List.of("true: y=0", "false: y=5"), describeAll(cases));
    }

    @Test
    void reportsOutcomesThatCannotBeReached() {
        List<TestCase> cases = generate(inputs("a", "b"),
                "int main(void) {",
                "    if (a > b) {",
                "        if (b > a) {",
                "        }",
                "    }",
                "}");

        assertEquals("true: no values: this outcome contradicts a condition on the way here", describe(cases.get(2)));
        assertEquals("false: a=1, b=0", describe(cases.get(3)));
    }

    @Test
    void reportsConditionsItCannotSolve() {
        Map<String, DeclaredVariable> declared = inputs("a", "b");
        declared.put("n", DeclaredVariable.computed("n"));

        List<TestCase> cases = generate(declared,
                "int main(void) {",
                "    if ((a + b) > 3) {",
                "        if (a > 0) {",
                "        }",
                "    }",
                "    if (n > 0) {",
                "    }",
                "    if (a > b && b > 0) {",
                "    }",
                "}");

        assertEquals("true: no values: the condition is too complex to solve", describe(cases.get(0)));
        assertEquals("true: no values: the condition on line 2, which leads here, is too complex to solve",
                describe(cases.get(2)));
        assertEquals("true: no values: depends on n, which is computed in the code", describe(cases.get(4)));
        assertEquals("true: no values: the condition is too complex to solve", describe(cases.get(6)));
    }

    @Test
    void undeclaredVariablesAreInputs() {
        List<TestCase> cases = generate(new LinkedHashMap<>(), "int main(void) {", "    if (z <= -5) {", "    }", "}");

        assertEquals(List.of("true: z=-5", "false: z=0"), describeAll(cases));
    }

    @Test
    void everyGeneratedCaseReachesItsConditionWithTheRightOutcome() {
        List<String> lines = program("sample.c");
        List<TestCase> cases = TestCaseGenerator.generate(ConditionPaths.find(ParsedSource.of(lines)), inputs("a", "b", "c", "k"));

        for (TestCase testCase : cases) {
            Map<String, Integer> values = testCase.inputs().orElseThrow();
            for (ConditionSite.Requirement step : testCase.site().path()) {
                assertEquals(step.outcome(), holds(step.condition(), values), () -> describe(testCase));
            }
            assertEquals(testCase.outcome(), holds(testCase.site().condition(), values), () -> describe(testCase));
        }
        assertTrue(cases.size() == 8);
    }

    /** Evaluates a two-operand comparison independently of the generator. */
    private static boolean holds(Condition condition, Map<String, Integer> values) {
        Condition.Compare comparison = (Condition.Compare) condition;
        int left = value(comparison.left(), values);
        int right = value(comparison.right(), values);
        return switch (comparison.sign()) {
            case ">" -> left > right;
            case "==" -> left == right;
            default -> throw new AssertionError("unexpected operator " + comparison.sign());
        };
    }

    private static int value(String operand, Map<String, Integer> values) {
        return operand.matches("-?\\d+") ? Integer.parseInt(operand) : values.get(operand);
    }
}
