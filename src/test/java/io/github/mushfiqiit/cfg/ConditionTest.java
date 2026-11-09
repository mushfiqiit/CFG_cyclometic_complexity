package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;

/** Pins down how conditions are parsed from {@code if}, {@code else if} and {@code while} lines. */
class ConditionTest {

    @Test
    void parsesASimpleComparison() {
        Condition condition = Condition.from("\t    while(k>0){");

        assertEquals("k>0", condition.getText());
        assertEquals(">", condition.getComparisonSign());
        assertEquals(List.of("k", "0"), condition.getOperands());
    }

    @Test
    void parsesSpacedOperatorsAndElseIf() {
        Condition condition = Condition.from("    else if (a <= b) {");

        assertEquals("a <= b", condition.getText());
        assertEquals("<=", condition.getComparisonSign());
        assertEquals(List.of("a", "b"), condition.getOperands());
    }

    @Test
    void conditionWithoutOperatorHasNoComparisonSign() {
        Condition condition = Condition.from("if(a){");

        assertNull(condition.getComparisonSign());
        assertEquals(List.of("a"), condition.getOperands());
    }

    @Test
    void emptyConditionHasNoTextOrOperands() {
        Condition condition = Condition.from("if(){");

        assertEquals("", condition.getText());
        assertTrue(condition.getOperands().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"if(a>b>=c){", "if((a+b)>c){", "if (a > b && b > 0) {", "if (f(x) > 0) {",
            "if (a[0] == 1) {", "if (!(a > b)) {", "if (a = b) {"})
    void complexConditionsKeepOnlyTheirText(String line) {
        Condition condition = Condition.from(line);

        assertTrue(line.contains(condition.getText()), condition.getText());
        assertNull(condition.getComparisonSign());
        assertTrue(condition.getOperands().isEmpty());
    }

    @Test
    void negationAndNegativeNumbersAreRead() {
        Condition negation = Condition.from("if (! done) {");
        Condition negative = Condition.from("if (a < - 5) {");

        assertEquals("!", negation.getComparisonSign());
        assertEquals(List.of("done"), negation.getOperands());
        assertEquals("<", negative.getComparisonSign());
        assertEquals(List.of("a", "-5"), negative.getOperands());
    }

    @Test
    void operandsCannotBeChangedFromOutside() {
        Condition condition = Condition.from("if(a>b){");

        assertThrows(UnsupportedOperationException.class,
                () -> condition.getOperands().add("c"));
    }
}
