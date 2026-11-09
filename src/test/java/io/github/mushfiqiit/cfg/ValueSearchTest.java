package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mushfiqiit.cfg.ValueSearch.Result;
import io.github.mushfiqiit.cfg.ValueSearch.Solved;
import io.github.mushfiqiit.cfg.ValueSearch.TooLarge;
import io.github.mushfiqiit.cfg.ValueSearch.Unsolvable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Checks that the search finds the simplest values, and reports when there are none. */
class ValueSearchTest {

    /** Reads a comparison such as {@code a > b} from its parts. */
    private static Comparison comparison(String left, String symbol, String right) {
        return Comparison.of(left, Operator.fromSymbol(symbol).orElseThrow(), right).orElseThrow();
    }

    private static Map<Comparison, Boolean> require(Object... comparisonsAndOutcomes) {
        Map<Comparison, Boolean> constraints = new LinkedHashMap<>();
        for (int i = 0; i < comparisonsAndOutcomes.length; i += 2) {
            constraints.put((Comparison) comparisonsAndOutcomes[i], (Boolean) comparisonsAndOutcomes[i + 1]);
        }
        return constraints;
    }

    private static Map<String, Integer> zeros(String... names) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String name : names) {
            values.put(name, 0);
        }
        return values;
    }

    @Test
    void prefersTheSimplestValues() {
        Result result = ValueSearch.search(require(comparison("a", ">", "b"), true), List.of("a", "b"), zeros("a", "b"));

        assertEquals(new Solved(Map.of("a", 1, "b", 0)), result);
    }

    @Test
    void triesTheNeighboursOfEachConstant() {
        Result result = ValueSearch.search(require(comparison("a", ">", "2"), true), List.of("a"), zeros("a"));

        assertEquals(Map.of("a", 3), assertInstanceOf(Solved.class, result).values());
    }

    @Test
    void keepsVariablesThatAreNotFree() {
        Map<String, Integer> start = zeros("y");
        start.put("x", 5);

        Result result = ValueSearch.search(require(comparison("x", ">", "y"), false), List.of("y"), start);

        assertEquals(Map.of("x", 5, "y", 5), assertInstanceOf(Solved.class, result).values());
    }

    @Test
    void reportsWhenNoValuesWork() {
        Result result = ValueSearch.search(
                require(comparison("a", ">", "b"), true, comparison("b", ">", "a"), true),
                List.of("a", "b"), zeros("a", "b"));

        assertEquals(new Unsolvable(), result);
    }

    /** With start values of 0 and no constants, each variable has 5 candidates (-2 to 2). */
    @Test
    void searchLimitIsCountedExactly() {
        Map<Comparison, Boolean> constraints = require(comparison("a", "<", "b"), true);
        String[] seven = {"a", "b", "c", "d", "e", "f", "g"};
        String[] eight = {"a", "b", "c", "d", "e", "f", "g", "h"};

        assertInstanceOf(Solved.class, ValueSearch.search(constraints, List.of(seven), zeros(seven)));
        assertEquals(new TooLarge(), ValueSearch.search(constraints, List.of(eight), zeros(eight)));
    }

    @Test
    void refusesSearchesThatAreTooLarge() {
        String[] names = {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
        Map<Comparison, Boolean> constraints = new LinkedHashMap<>();
        for (int i = 0; i + 1 < names.length; i++) {
            constraints.put(comparison(names[i], "<", names[i + 1]), true);
        }

        Result result = ValueSearch.search(constraints, List.of(names), zeros(names));

        assertEquals(new TooLarge(), result);
    }

    @Test
    void succeedsWithoutFreeVariablesWhenTheConstraintsAlreadyHold() {
        Result result = ValueSearch.search(require(comparison("x", "==", "5"), true), List.of(), Map.of("x", 5));

        assertEquals(Map.of("x", 5), assertInstanceOf(Solved.class, result).values());
    }

    @Test
    void neighboursOfTheLargestIntDoNotWrapAround() {
        Result result = ValueSearch.search(require(comparison("a", "<", "2147483647"), true), List.of("a"), zeros("a"));

        assertEquals(Map.of("a", 0), assertInstanceOf(Solved.class, result).values());
    }

    @Test
    void variablesNeedAStartingValueUnlessTheSearchSetsThem() {
        Map<Comparison, Boolean> constraints = require(comparison("a", ">", "b"), true);

        assertInstanceOf(Solved.class, ValueSearch.search(constraints, List.of("a", "b"), Map.of()));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ValueSearch.search(constraints, List.of("a"), Map.of()));
        assertEquals("no starting value for variable b", failure.getMessage());
    }
}
