package cfg;

import java.util.List;

/** Counts the decision points in source lines, which gives the cyclomatic complexity. */
public final class ComplexityCalculator {

    /** Prevents instantiation; this class only has static methods. */
    private ComplexityCalculator() {
    }

    /**
     * Counts the {@code if}, {@code else if} and loop lines. Plain {@code else} lines are not
     * counted, because they are not decision points.
     *
     * @param lines the source lines to scan
     * @return the counts, whose {@link DecisionCounts#complexity()} is the cyclomatic complexity
     */
    public static DecisionCounts countDecisions(List<String> lines) {
        int ifs = 0;
        int elseIfs = 0;
        int loops = 0;

        for (String line : lines) {
            if (SyntaxIdentifier.isElseIf(line)) {
                elseIfs++;
            } else if (SyntaxIdentifier.isIf(line)) {
                ifs++;
            } else if (SyntaxIdentifier.isLoop(line)) {
                loops++;
            }
        }
        return new DecisionCounts(ifs, elseIfs, loops);
    }
}
