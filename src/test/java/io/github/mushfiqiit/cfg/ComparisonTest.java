package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mushfiqiit.cfg.Operand.Constant;
import io.github.mushfiqiit.cfg.Operand.Variable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks how conditions become comparisons, and how comparisons are evaluated. */
class ComparisonTest {

    private static Optional<Comparison> comparisonOf(String line) {
        return Condition.from(line).toComparison();
    }

    private static Comparison comparison(String left, Operator operator, String right) {
        return Comparison.of(left, operator, right).orElseThrow();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "while(k>0){          | k  | >  | 0",
        "if (a <= b) {        | a  | <= | b",
        "else if (5 == x) {   | 5  | == | x",
        "if (z != -3) {       | z  | != | -3",
        "if (a) {             | a  | != | 0",
        "if (!a) {            | a  | == | 0"})
    void simpleConditionsBecomeComparisons(String line, String left, String symbol, String right) {
        Operator operator = Operator.fromSymbol(symbol).orElseThrow();

        assertEquals(Optional.of(comparison(left, operator, right)), comparisonOf(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {"if ((a + b) > 3) {", "if (a > b && b > 0) {", "if (a > b >= c) {", "if (f(x) > 0) {",
            "if () {", "if (a !b < c) {"})
    void complexConditionsCannotBeEvaluated(String line) {
        assertEquals(Optional.empty(), comparisonOf(line));
    }

    @Test
    void operandsAreReadOnceIntoVariablesAndConstants() {
        assertEquals(new Comparison(new Variable("a"), Operator.LESS, new Constant(-10)),
                comparison("a", Operator.LESS, "-10"));
    }

    @Test
    void listsItsVariablesAndConstants() {
        Comparison comparison = comparison("a", Operator.LESS, "10");

        assertEquals(List.of("a"), comparison.variables());
        assertEquals(List.of(10), comparison.constants());
        assertEquals(List.of("a", "b"), comparison("a", Operator.GREATER, "b").variables());
        assertEquals(List.of(1, 2), comparison("1", Operator.GREATER, "2").constants());
    }

    @ParameterizedTest
    @CsvSource({"<, 1, 2, true", "<, 2, 2, false", "<=, 2, 2, true", ">, 3, 2, true", ">=, 1, 2, false",
            "==, 2, 2, true", "!=, 2, 2, false"})
    void evaluatesEveryOperator(String symbol, int a, int b, boolean expected) {
        Operator operator = Operator.fromSymbol(symbol).orElseThrow();

        assertEquals(expected, comparison("a", operator, "b").holds(Map.of("a", a, "b", b)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2count", "5a", "a b", "", "- 5"})
    void textThatIsNeitherAVariableNorAnIntegerIsNotAnOperand(String text) {
        assertEquals(Optional.empty(), Operand.parse(text));
        assertEquals(Optional.empty(), Comparison.of(text, Operator.LESS, "1"));
    }

    @Test
    void integersMustFitInAnInt() {
        assertEquals(Optional.of(new Constant(2147483647)), Operand.parse("2147483647"));
        assertEquals(Optional.of(new Constant(-2147483648)), Operand.parse("-2147483648"));
        assertEquals(Optional.of(new Constant(7)), Operand.parse("0007"));
        assertEquals(Optional.empty(), Operand.parse("2147483648"));
        assertEquals(Optional.empty(), Operand.parse("99999999999999999999"));
        assertEquals(Optional.empty(), comparisonOf("if (a > 99999999999) {"));
    }

    @Test
    void evaluatingWithoutAVariablesValueIsRejected() {
        Comparison comparison = comparison("a", Operator.GREATER, "b");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> comparison.holds(Map.of("a", 1)));
        assertEquals("no value for variable b", failure.getMessage());
    }
}
