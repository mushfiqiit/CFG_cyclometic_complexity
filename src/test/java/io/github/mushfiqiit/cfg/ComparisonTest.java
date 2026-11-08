package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "while(k>0){          | k  | >  | 0",
        "if (a <= b) {        | a  | <= | b",
        "else if (5 == x) {   | 5  | == | x",
        "if (z != -3) {       | z  | != | -3",
        "if (a) {             | a  | != | 0",
        "if (!a) {            | a  | == | 0"})
    void simpleConditionsBecomeComparisons(String line, String left, String operator, String right) {
        assertEquals(Optional.of(new Comparison(left, operator, right)), comparisonOf(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {"if ((a + b) > 3) {", "if (a > b && b > 0) {", "if (a > b >= c) {", "if (f(x) > 0) {",
            "if () {", "if (a !b < c) {"})
    void complexConditionsCannotBeEvaluated(String line) {
        assertEquals(Optional.empty(), comparisonOf(line));
    }

    @Test
    void listsItsVariablesAndConstants() {
        Comparison comparison = new Comparison("a", "<", "10");

        assertEquals(List.of("a"), comparison.variables());
        assertEquals(List.of(10), comparison.constants());
        assertEquals(List.of("a", "b"), new Comparison("a", ">", "b").variables());
    }

    @ParameterizedTest
    @CsvSource({"<, 1, 2, true", "<, 2, 2, false", "<=, 2, 2, true", ">, 3, 2, true", ">=, 1, 2, false",
            "==, 2, 2, true", "!=, 2, 2, false"})
    void evaluatesEveryOperator(String operator, int a, int b, boolean expected) {
        assertEquals(expected, new Comparison("a", operator, "b").holds(Map.of("a", a, "b", b)));
    }

    @Test
    void recognizesVariablesAndIntegers() {
        assertTrue(Comparison.isVariable("count_2"));
        assertFalse(Comparison.isVariable("2count"));
        assertTrue(Comparison.isInteger("-5"));
        assertFalse(Comparison.isInteger("5a"));
    }

    @Test
    void integersMustFitInAnInt() {
        assertEquals(java.util.OptionalInt.of(2147483647), Comparison.parseInteger("2147483647"));
        assertEquals(java.util.OptionalInt.of(-2147483648), Comparison.parseInteger("-2147483648"));
        assertEquals(java.util.OptionalInt.of(7), Comparison.parseInteger("0007"));
        assertEquals(java.util.OptionalInt.empty(), Comparison.parseInteger("2147483648"));
        assertEquals(java.util.OptionalInt.empty(), Comparison.parseInteger("99999999999999999999"));
        assertEquals(Optional.empty(), comparisonOf("if (a > 99999999999) {"));
    }
}
