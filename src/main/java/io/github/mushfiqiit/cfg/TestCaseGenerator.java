package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.ConditionSite.Requirement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Generates test cases that make each condition true in one case and false in another, using
 * input values that reach the condition.
 *
 * <p>Conditions are solved when they compare two operands, each a variable or an integer, with
 * {@code <}, {@code <=}, {@code >}, {@code >=}, {@code ==} or {@code !=}, or test a single
 * variable, as in {@code if (a)} or {@code if (!a)}. Values are those at the start of the
 * function: assignments made before a condition is checked are not taken into account.
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

    /**
     * A solvable condition: two operands compared with an operator.
     *
     * @param left a variable name or an integer
     * @param operator one of {@code < <= > >= == !=}
     * @param right a variable name or an integer
     */
    private record Comparison(String left, String operator, String right) {

        /**
         * Evaluates the comparison.
         *
         * @param values the value of every variable it uses
         * @return whether the comparison holds
         */
        boolean holds(Map<String, Integer> values) {
            int a = valueOf(left, values);
            int b = valueOf(right, values);
            return switch (operator) {
                case "<" -> a < b;
                case "<=" -> a <= b;
                case ">" -> a > b;
                case ">=" -> a >= b;
                case "==" -> a == b;
                default -> a != b;
            };
        }

        /**
         * Returns an operand's value.
         *
         * @param operand a variable name or an integer
         * @param values the value of every variable
         * @return the integer itself, or the variable's value
         */
        private static int valueOf(String operand, Map<String, Integer> values) {
            return INTEGER.matcher(operand).matches() ? Integer.parseInt(operand) : values.get(operand);
        }
    }

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    private static final Set<String> OPERATORS = Set.of("<", "<=", ">", ">=", "==", "!=");
    private static final List<Integer> SMALL_VALUES = List.of(0, 1, -1, 2, -2);
    private static final int SEARCH_LIMIT = 200_000;

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
                if (IDENTIFIER.matcher(operand).matches() && !declared.containsKey(operand)) {
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
            Optional<Comparison> comparison = comparisonOf(requirement.condition());
            if (comparison.isEmpty()) {
                return noValues(site, outcome, requirement.lineNumber() == site.lineNumber()
                        ? "the condition is too complex to solve"
                        : "the condition on line " + requirement.lineNumber() + ", which leads here, is too complex to solve");
            }
            Optional<String> computed = computedOperand(comparison.get(), declared);
            if (computed.isPresent()) {
                return noValues(site, outcome, "depends on " + computed.get() + ", which is computed in the code");
            }
            if (constraints.containsKey(comparison.get()) && constraints.get(comparison.get()) != requirement.outcome()) {
                return noValues(site, outcome, "this outcome contradicts a condition on the way here");
            }
            constraints.put(comparison.get(), requirement.outcome());
        }
        return solve(site, outcome, constraints, inputs, declared);
    }

    // ***************************************************************************
    // Reading conditions
    // ***************************************************************************

    /**
     * Turns a condition into a comparison that can be evaluated.
     *
     * @param condition the condition
     * @return the comparison, or an empty value if the condition is too complex
     */
    private static Optional<Comparison> comparisonOf(Condition condition) {
        List<String> operands = condition.getOperands();
        String operator = condition.getComparisonSign();
        Comparison comparison;
        if (operands.size() == 1 && operator == null) {
            comparison = new Comparison(operands.get(0), "!=", "0");
        } else if (operands.size() == 1 && operator.equals("!")) {
            comparison = new Comparison(operands.get(0), "==", "0");
        } else if (operands.size() == 2 && operator != null && OPERATORS.contains(operator)) {
            comparison = new Comparison(operands.get(0), operator, operands.get(1));
        } else {
            return Optional.empty();
        }
        return isSimpleOperand(comparison.left()) && isSimpleOperand(comparison.right())
                ? Optional.of(comparison) : Optional.empty();
    }

    /**
     * Tells whether an operand is a variable name or an integer.
     *
     * @param operand the operand's text
     * @return true for a variable name or an integer
     */
    private static boolean isSimpleOperand(String operand) {
        return IDENTIFIER.matcher(operand).matches() || INTEGER.matcher(operand).matches();
    }

    /**
     * Finds an operand whose value the code computes, so no test case can choose it.
     *
     * @param comparison the comparison to check
     * @param declared the declared variables, by name
     * @return the first such variable's name, if any
     */
    private static Optional<String> computedOperand(Comparison comparison, Map<String, DeclaredVariable> declared) {
        for (String operand : List.of(comparison.left(), comparison.right())) {
            DeclaredVariable variable = declared.get(operand);
            if (variable != null && variable.source() == DeclaredVariable.Source.COMPUTED) {
                return Optional.of(operand);
            }
        }
        return Optional.empty();
    }

    // ***************************************************************************
    // Searching for values
    // ***************************************************************************

    /**
     * Searches for input values that give every comparison its required outcome. Combinations of
     * the simplest values are tried first, then the search widens one candidate value at a time.
     * Inputs that no comparison uses are 0.
     *
     * @param site the condition being covered
     * @param outcome the outcome being covered
     * @param constraints each comparison with its required outcome
     * @param inputs the input variable names, in report order
     * @param declared the declared variables, by name
     * @return the test case
     */
    private static TestCase solve(ConditionSite site, boolean outcome, Map<Comparison, Boolean> constraints,
            List<String> inputs, Map<String, DeclaredVariable> declared) {
        Map<String, Integer> values = new LinkedHashMap<>();
        inputs.forEach(name -> values.put(name, 0));
        declared.values().stream()
                .filter(variable -> variable.source() == DeclaredVariable.Source.FIXED)
                .forEach(variable -> values.put(variable.name(), variable.value()));

        List<String> free = new ArrayList<>();
        for (Comparison comparison : constraints.keySet()) {
            for (String operand : List.of(comparison.left(), comparison.right())) {
                if (IDENTIFIER.matcher(operand).matches() && inputs.contains(operand) && !free.contains(operand)) {
                    free.add(operand);
                }
            }
        }
        List<Integer> candidates = candidates(constraints.keySet(), values);
        long combinations = (long) Math.pow(candidates.size(), free.size());
        if (combinations > SEARCH_LIMIT) {
            return noValues(site, outcome, "too many variables to search; no values were tried");
        }

        for (int width = 1; width <= candidates.size(); width++) {
            int[] choice = new int[free.size()];
            do {
                if (!usesCandidate(choice, width - 1)) {
                    continue; // Every value in this combination was already tried with a smaller width.
                }
                for (int i = 0; i < free.size(); i++) {
                    values.put(free.get(i), candidates.get(choice[i]));
                }
                if (constraints.entrySet().stream().allMatch(entry -> entry.getKey().holds(values) == entry.getValue())) {
                    Map<String, Integer> chosen = new LinkedHashMap<>();
                    inputs.forEach(name -> chosen.put(name, values.get(name)));
                    return new TestCase(site, outcome, Optional.of(chosen), "");
                }
            } while (advance(choice, width));
        }
        return noValues(site, outcome, "this outcome contradicts a condition on the way here");
    }

    /**
     * Chooses the values to try: small numbers, and each number in the comparisons or held by a
     * fixed variable together with its neighbours. Values closer to 0 come first, positive before
     * negative.
     *
     * @param comparisons the comparisons to satisfy
     * @param fixedValues the values of the variables that are already set
     * @return the candidate values, without duplicates
     */
    private static List<Integer> candidates(Set<Comparison> comparisons, Map<String, Integer> fixedValues) {
        Set<Integer> seeds = new TreeSet<>(SMALL_VALUES);
        for (Comparison comparison : comparisons) {
            for (String operand : List.of(comparison.left(), comparison.right())) {
                Integer seed = INTEGER.matcher(operand).matches() ? Integer.valueOf(operand) : fixedValues.get(operand);
                if (seed != null) {
                    seeds.addAll(List.of(seed - 1, seed, seed + 1));
                }
            }
        }
        List<Integer> ordered = new ArrayList<>(seeds);
        ordered.sort(Comparator.<Integer>comparingInt(Math::abs).thenComparing(value -> value < 0));
        return ordered;
    }

    /**
     * Moves to the next combination of candidate indexes, like an odometer whose last digit turns
     * fastest.
     *
     * @param choice the index chosen for each variable; changed in place
     * @param base the number of candidates each variable can take
     * @return false once every combination has been visited
     */
    private static boolean advance(int[] choice, int base) {
        for (int i = choice.length - 1; i >= 0; i--) {
            choice[i]++;
            if (choice[i] < base) {
                return true;
            }
            choice[i] = 0;
        }
        return false;
    }

    /**
     * Tells whether a combination uses a given candidate index, so combinations are tried in
     * widening rings: first those using only the first candidate, then those that also use the
     * second, and so on.
     *
     * @param choice the index chosen for each variable
     * @param index the candidate index to look for
     * @return true if some variable uses it, or if there are no variables and it is the first
     */
    private static boolean usesCandidate(int[] choice, int index) {
        if (choice.length == 0) {
            return index == 0;
        }
        for (int chosen : choice) {
            if (chosen == index) {
                return true;
            }
        }
        return false;
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
