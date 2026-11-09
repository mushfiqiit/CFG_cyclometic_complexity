package io.github.mushfiqiit.cfg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Searches for variable values that give each comparison a required outcome, preferring the
 * simplest values. Combinations of the simplest candidates are tried first, then the search widens
 * one candidate at a time.
 */
final class ValueSearch {

    /** What a search found: values that work, or why there are none. */
    sealed interface Result permits Solved, Unsolvable, TooLarge {
    }

    /**
     * Values that give every comparison its required outcome.
     *
     * @param values every variable's value
     */
    record Solved(Map<String, Integer> values) implements Result {

        /**
         * Creates a result, keeping a read-only copy of the values.
         *
         * @param values every variable's value
         */
        Solved {
            values = Map.copyOf(values);
        }
    }

    /** Every combination was tried and none works. */
    record Unsolvable() implements Result {
    }

    /** There were too many combinations, so none were tried. */
    record TooLarge() implements Result {
    }

    private static final List<Integer> SMALL_VALUES = List.of(0, 1, -1, 2, -2);
    private static final int SEARCH_LIMIT = 200_000;

    /** Prevents instantiation; this class only has static methods. */
    private ValueSearch() {
    }

    // ***************************************************************************
    // Searching
    // ***************************************************************************

    /**
     * Searches for values of the free variables that give every comparison its required outcome.
     * The other variables keep their starting values.
     *
     * @param constraints each comparison with its required outcome
     * @param free the variables the search may change, in the order to vary them
     * @param startValues every variable's starting value, including the fixed ones
     * @return the values found, or why there are none
     * @throws IllegalArgumentException if a comparison uses a variable that is neither free nor
     *         given a starting value
     */
    static Result search(Map<Comparison, Boolean> constraints, List<String> free, Map<String, Integer> startValues) {
        Set<String> freeNames = Set.copyOf(free);
        for (Comparison comparison : constraints.keySet()) {
            for (String variable : comparison.variables()) {
                if (!freeNames.contains(variable) && startValues.get(variable) == null) {
                    throw new IllegalArgumentException("no starting value for variable " + variable);
                }
            }
        }
        List<Integer> candidates = candidates(constraints.keySet(), startValues);
        if (exceedsSearchLimit(candidates.size(), free.size())) {
            return new TooLarge();
        }
        Map<String, Integer> values = new LinkedHashMap<>(startValues);
        for (int width = 1; width <= candidates.size(); width++) {
            int[] choice = new int[free.size()];
            do {
                if (!usesCandidate(choice, width - 1)) {
                    continue; // Every value in this combination was already tried with a smaller width.
                }
                for (int i = 0; i < free.size(); i++) {
                    values.put(free.get(i), candidates.get(choice[i]));
                }
                if (allHold(constraints, values)) {
                    return new Solved(values);
                }
            } while (advance(choice, width));
        }
        return new Unsolvable();
    }

    /**
     * Tells whether trying every combination of values would go over {@link #SEARCH_LIMIT}. The
     * count is worked out exactly, and stops growing as soon as it passes the limit, so it never
     * overflows.
     *
     * @param candidateCount the number of values each free variable can take
     * @param freeCount the number of free variables
     * @return true if {@code candidateCount} to the power {@code freeCount} is over the limit
     */
    private static boolean exceedsSearchLimit(int candidateCount, int freeCount) {
        long combinations = 1;
        for (int i = 0; i < freeCount; i++) {
            combinations *= candidateCount;
            if (combinations > SEARCH_LIMIT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether every comparison has its required outcome.
     *
     * @param constraints each comparison with its required outcome
     * @param values every variable's value
     * @return true if all of them hold
     */
    private static boolean allHold(Map<Comparison, Boolean> constraints, Map<String, Integer> values) {
        return constraints.entrySet().stream().allMatch(entry -> entry.getKey().holds(values) == entry.getValue());
    }

    // ***************************************************************************
    // Choosing candidate values
    // ***************************************************************************

    /**
     * Chooses the values to try: small numbers, and each number in the comparisons or held by a
     * variable they use, together with its neighbours that fit in an {@code int}. Values closer to
     * 0 come first, positive before negative.
     *
     * @param comparisons the comparisons to satisfy
     * @param startValues every variable's starting value
     * @return the candidate values, without duplicates
     */
    private static List<Integer> candidates(Set<Comparison> comparisons, Map<String, Integer> startValues) {
        Set<Integer> seeds = new TreeSet<>(SMALL_VALUES);
        for (Comparison comparison : comparisons) {
            List<Integer> numbers = new ArrayList<>(comparison.constants());
            comparison.variables().stream().map(startValues::get).filter(Objects::nonNull).forEach(numbers::add);
            for (int number : numbers) {
                for (long neighbour = number - 1L; neighbour <= number + 1L; neighbour++) {
                    if (neighbour >= Integer.MIN_VALUE && neighbour <= Integer.MAX_VALUE) {
                        seeds.add((int) neighbour);
                    }
                }
            }
        }
        List<Integer> ordered = new ArrayList<>(seeds);
        ordered.sort(Comparator.<Integer>comparingLong(value -> Math.abs((long) value)).thenComparing(value -> value < 0));
        return ordered;
    }

    // ***************************************************************************
    // Stepping through combinations
    // ***************************************************************************

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
}
