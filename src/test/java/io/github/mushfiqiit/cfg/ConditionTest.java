package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

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

    @Test
    void lastOperatorWinsWhenThereAreSeveral() {
        Condition condition = Condition.from("if(a>b>=c){");

        assertEquals(">=", condition.getComparisonSign());
        assertEquals(List.of("a", "b", "c"), condition.getOperands());
    }

    @Test
    void operandsCannotBeChangedFromOutside() {
        Condition condition = Condition.from("if(a>b){");

        assertThrows(UnsupportedOperationException.class,
                () -> condition.getOperands().add("c"));
    }
}
