package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mushfiqiit.cfg.Condition.Compare;
import io.github.mushfiqiit.cfg.Condition.Negation;
import io.github.mushfiqiit.cfg.Condition.Truthy;
import io.github.mushfiqiit.cfg.Condition.Unsolvable;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Pins down how conditions are parsed from {@code if}, {@code else if} and {@code while} lines. */
class ConditionTest {

    @Test
    void parsesASimpleComparison() {
        Condition condition = Condition.from("\t    while(k>0){");

        assertEquals(new Compare("k>0", "k", Operator.GREATER, "0"), condition);
        assertEquals(Optional.of(">"), condition.operator());
        assertEquals(List.of("k", "0"), condition.operands());
    }

    @Test
    void parsesSpacedOperatorsAndElseIf() {
        assertEquals(new Compare("a <= b", "a", Operator.LESS_OR_EQUAL, "b"), Condition.from("    else if (a <= b) {"));
    }

    @Test
    void singleOperandHasNoOperator() {
        Condition condition = Condition.from("if(a){");

        assertEquals(new Truthy("a", "a"), condition);
        assertEquals(Optional.empty(), condition.operator());
        assertEquals(List.of("a"), condition.operands());
    }

    @Test
    void emptyConditionHasNoTextOrOperands() {
        Condition condition = Condition.from("if(){");

        assertEquals(new Unsolvable(""), condition);
        assertTrue(condition.operands().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"if(a>b>=c){", "if((a+b)>c){", "if (a > b && b > 0) {", "if (f(x) > 0) {",
            "if (a[0] == 1) {", "if (!(a > b)) {", "if (a = b) {"})
    void complexConditionsKeepOnlyTheirText(String line) {
        Condition condition = Condition.from(line);

        assertTrue(condition instanceof Unsolvable, condition::toString);
        assertTrue(line.contains(condition.text()), condition.text());
        assertEquals(Optional.empty(), condition.operator());
        assertTrue(condition.operands().isEmpty());
        assertEquals(Optional.empty(), condition.toComparison());
    }

    @Test
    void negationAndNegativeNumbersAreRead() {
        Condition negation = Condition.from("if (! done) {");

        assertEquals(new Negation("! done", "done"), negation);
        assertEquals(Optional.of("!"), negation.operator());
        assertEquals(new Compare("a < - 5", "a", Operator.LESS, "-5"), Condition.from("if (a < - 5) {"));
    }

    @Test
    void eachKindTurnsIntoItsComparison() {
        assertEquals(Comparison.of("a", Operator.LESS, "b"), Condition.from("if (a < b) {").toComparison());
        assertEquals(Comparison.of("done", Operator.EQUAL, "0"), Condition.from("if (!done) {").toComparison());
        assertEquals(Comparison.of("flag", Operator.NOT_EQUAL, "0"), Condition.from("if (flag) {").toComparison());
    }

    @Test
    void operandsCannotBeChangedFromOutside() {
        Condition condition = Condition.from("if(a>b){");

        assertThrows(UnsupportedOperationException.class, () -> condition.operands().add("c"));
    }
}
