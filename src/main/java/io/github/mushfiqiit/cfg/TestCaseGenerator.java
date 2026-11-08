package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.ConditionSite.Requirement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Generates test cases that make each condition true in one case and false in another, using
 * input values that reach the condition.
 *
 * <p>Each test case requires the conditions on the way to its condition to have the outcomes that
 * lead there, and its condition to have the outcome being covered. Conditions must be simple
 * enough to evaluate, as described by {@link Comparison}; {@link ValueSearch} then finds the
 * values. Values are those at the start of the function: assignments made before a condition is
 * checked are not taken into account.
 */
final class TestCaseGenerator {

    /**
     * A test case for one outcome of one condition.
     *
     * @param site the condition the test case covers
     * @param outcome whether the test case makes the condition true or false
     * @param inputs the value of each input variable, in report order; empty if no values were
     *        found
     * @param reason why no values were found; empty when there are inputs
     */
    record TestCase(ConditionSite site, boolean outcome, Optional<Map<String, Integer>> inputs, String reason) {
    }

    private static final String CONTRADICTION = "this outcome contradicts a condition on the way here";

    /** Prevents instantiation; this class only has static methods. */
    private TestCaseGenerator() {
    }

    // ***************************************************************************
    // Generating test cases
    // ***************************************************************************

    /**
     * Generates two test cases per condition: one where it is true and one where it is false.
     *
     * @param sites the conditions with the paths that lead to them
     * @param declared the function's declared {@code int} variables, by name
     * @return the test cases, in condition order with the true outcome first
     */
    static List<TestCase> generate(List<ConditionSite> sites, Map<String, DeclaredVariable> declared) {
        List<String> inputs = inputNames(sites, declared);
        List<TestCase> cases = new ArrayList<>();
        for (ConditionSite site : sites) {
            for (boolean outcome : List.of(true, false)) {
                cases.add(generate(site, outcome, inputs, declared));
            }
        }
        return cases;
    }

    /**
     * Lists the variables a test case chooses: declared inputs, then variables used in conditions
     * without being declared.
     *
     * @param sites the conditions
     * @param declared the declared variables, by name
     * @return the input variable names, in report order
     */
    static List<String> inputNames(List<ConditionSite> sites, Map<String, DeclaredVariable> declared) {
        Set<String> names = new LinkedHashSet<>();
        declared.values().stream()
                .filter(variable -> variable.source() == DeclaredVariable.Source.INPUT)
                .forEach(variable -> names.add(variable.name()));
        for (ConditionSite site : sites) {
            for (String operand : site.condition().getOperands()) {
                if (Comparison.isVariable(operand) && !declared.containsKey(operand)) {
                    names.add(operand);
                }
            }
        }
        return List.copyOf(names);
    }

    /**
     * Generates the test case for one outcome of one condition.
     *
     * @param site the condition
     * @param outcome the outcome to cover
     * @param inputs the input variable names, in report order
     * @param declared the declared variables, by name
     * @return the test case, with inputs or with the reason there are none
     */
    private static TestCase generate(ConditionSite site, boolean outcome, List<String> inputs,
            Map<String, DeclaredVariable> declared) {
        List<Requirement> requirements = new ArrayList<>(site.path());
        requirements.add(new Requirement(site.lineNumber(), site.condition(), outcome));

        Map<Comparison, Boolean> constraints = new LinkedHashMap<>();
        for (Requirement requirement : requirements) {
            Optional<Comparison> solvable = requirement.condition().toComparison();
            if (solvable.isEmpty()) {
                return noValues(site, outcome, requirement.lineNumber() == site.lineNumber()
                        ? "the condition is too complex to solve"
                        : "the condition on line " + requirement.lineNumber() + ", which leads here, is too complex to solve");
            }
            Comparison comparison = solvable.orElseThrow();
            Optional<String> computed = computedVariable(comparison, declared);
            if (computed.isPresent()) {
                return noValues(site, outcome, "depends on " + computed.orElseThrow() + ", which is computed in the code");
            }
            Boolean required = constraints.put(comparison, requirement.outcome());
            if (required != null && required != requirement.outcome()) {
                return noValues(site, outcome, CONTRADICTION);
            }
        }
        return search(site, outcome, constraints, inputs, declared);
    }

    // ***************************************************************************
    // Finding values
    // ***************************************************************************

    /**
     * Finds input values for a test case. Inputs start at 0, fixed variables keep their declared
     * values, and only the inputs the comparisons use are varied.
     *
     * @param site the condition being covered
     * @param outcome the outcome being covered
     * @param constraints each comparison with its required outcome
     * @param inputs the input variable names, in report order
     * @param declared the declared variables, by name
     * @return the test case
     */
    private static TestCase search(ConditionSite site, boolean outcome, Map<Comparison, Boolean> constraints,
            List<String> inputs, Map<String, DeclaredVariable> declared) {
        Map<String, Integer> startValues = new LinkedHashMap<>();
        inputs.forEach(name -> startValues.put(name, 0));
        declared.values().stream()
                .filter(variable -> variable.source() == DeclaredVariable.Source.FIXED)
                .forEach(variable -> startValues.put(variable.name(), variable.value()));

        List<String> free = new ArrayList<>();
        for (Comparison comparison : constraints.keySet()) {
            for (String variable : comparison.variables()) {
                if (inputs.contains(variable) && !free.contains(variable)) {
                    free.add(variable);
                }
            }
        }

        ValueSearch.Result result = ValueSearch.search(constraints, free, startValues);
        return switch (result.status()) {
            case FOUND -> {
                Map<String, Integer> chosen = new LinkedHashMap<>();
                inputs.forEach(name -> chosen.put(name, result.values().orElseThrow().get(name)));
                yield new TestCase(site, outcome, Optional.of(chosen), "");
            }
            case NO_SOLUTION -> noValues(site, outcome, CONTRADICTION);
            case TOO_MANY_COMBINATIONS -> noValues(site, outcome, "too many variables to search; no values were tried");
        };
    }

    /**
     * Finds a variable in a comparison whose value the code computes, so no test case can
     * choose it.
     *
     * @param comparison the comparison to check
     * @param declared the declared variables, by name
     * @return the first such variable's name, if any
     */
    private static Optional<String> computedVariable(Comparison comparison, Map<String, DeclaredVariable> declared) {
        return comparison.variables().stream()
                .filter(name -> declared.containsKey(name)
                        && declared.get(name).source() == DeclaredVariable.Source.COMPUTED)
                .findFirst();
    }

    /**
     * Creates a test case without values.
     *
     * @param site the condition
     * @param outcome the outcome
     * @param reason why no values were found
     * @return the test case
     */
    private static TestCase noValues(ConditionSite site, boolean outcome, String reason) {
        return new TestCase(site, outcome, Optional.empty(), reason);
    }
}
