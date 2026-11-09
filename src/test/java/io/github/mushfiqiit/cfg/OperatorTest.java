package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks the list of comparison operators. */
class OperatorTest {

    @Test
    void everyOperatorIsFoundByItsSymbol() {
        for (Operator operator : Operator.values()) {
            assertEquals(Optional.of(operator), Operator.fromSymbol(operator.symbol()));
        }
        assertEquals(List.of("<", "<=", ">", ">=", "==", "!="),
                Arrays.stream(Operator.values()).map(Operator::symbol).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"=", "<>", "=<", "!", ""})
    void otherTextIsNotAnOperator(String symbol) {
        assertEquals(Optional.empty(), Operator.fromSymbol(symbol));
    }

    @Test
    void symbolPatternMatchesEachOperatorWhole() {
        for (Operator operator : Operator.values()) {
            assertTrue(Operator.SYMBOL.matcher(operator.symbol()).matches(), operator::symbol);
        }
        assertEquals("<=", Operator.SYMBOL.matcher("<=").results().findFirst().orElseThrow().group());
    }
}
