package io.github.mushfiqiit.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mushfiqiit.cfg.ValueSearch.Result;
import io.github.mushfiqiit.cfg.ValueSearch.Status;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Checks that the search finds the simplest values, and reports when there are none. */
class ValueSearchTest {

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
        Result result = ValueSearch.search(require(new Comparison("a", ">", "b"), true), List.of("a", "b"), zeros("a", "b"));

        assertEquals(Status.FOUND, result.status());
        assertEquals(Map.of("a", 1, "b", 0), result.values().orElseThrow());
    }

    @Test
    void triesTheNeighboursOfEachConstant() {
        Result result = ValueSearch.search(require(new Comparison("a", ">", "2"), true), List.of("a"), zeros("a"));

        assertEquals(Map.of("a", 3), result.values().orElseThrow());
    }

    @Test
    void keepsVariablesThatAreNotFree() {
        Map<String, Integer> start = zeros("y");
        start.put("x", 5);

        Result result = ValueSearch.search(require(new Comparison("x", ">", "y"), false), List.of("y"), start);

        assertEquals(Map.of("x", 5, "y", 5), result.values().orElseThrow());
    }

    @Test
    void reportsWhenNoValuesWork() {
        Result result = ValueSearch.search(
                require(new Comparison("a", ">", "b"), true, new Comparison("b", ">", "a"), true),
                List.of("a", "b"), zeros("a", "b"));

        assertEquals(new Result(Status.NO_SOLUTION, Optional.empty()), result);
    }

    @Test
    void refusesSearchesThatAreTooLarge() {
        String[] names = {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
        Map<Comparison, Boolean> constraints = new LinkedHashMap<>();
        for (int i = 0; i + 1 < names.length; i++) {
            constraints.put(new Comparison(names[i], "<", names[i + 1]), true);
        }

        Result result = ValueSearch.search(constraints, List.of(names), zeros(names));

        assertEquals(Status.TOO_MANY_COMBINATIONS, result.status());
    }

    @Test
    void succeedsWithoutFreeVariablesWhenTheConstraintsAlreadyHold() {
        Result result = ValueSearch.search(require(new Comparison("x", "==", "5"), true), List.of(), Map.of("x", 5));

        assertEquals(Map.of("x", 5), result.values().orElseThrow());
    }
}
