package cfg;

import java.util.List;

/** Counts the branching keywords in source lines, which gives the cyclomatic complexity. */
public final class ComplexityCalculator {

    /** Prevents instantiation; this class only has static methods. */
    private ComplexityCalculator() {
    }

    /**
     * Counts the {@code if}, {@code else if}, {@code else} and loop lines.
     *
     * <p>{@code else if} lines are counted as {@code else}, so the {@code else if} count is always
     * 0. The total is unaffected.
     *
     * @param lines the source lines to scan
     * @return the counts, whose {@link DecisionCounts#complexity()} is the cyclomatic complexity
     */
    public static DecisionCounts countDecisions(List<String> lines) {
        int ifs = 0;
        int elseIfs = 0;
        int elses = 0;
        int loops = 0;

        for (String line : lines) {
            // isElse also matches "else if", so those lines are counted as elses and elseIfs stays 0.
            if (SyntaxIdentifier.isElse(line)) {
                elses++;
            } else if (SyntaxIdentifier.isElseIf(line)) {
                elseIfs++;
            } else if (SyntaxIdentifier.isIf(line)) {
                ifs++;
            } else if (SyntaxIdentifier.isLoop(line)) {
                loops++;
            }
        }
        return new DecisionCounts(ifs, elseIfs, elses, loops);
    }
}
