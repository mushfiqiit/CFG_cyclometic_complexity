package cfg;

import java.util.List;

/** Calculates cyclomatic complexity by counting branching keywords. */
public final class ComplexityCalculator {

    /** Prevents instantiation; this class only has static methods. */
    private ComplexityCalculator() {
    }

    /**
     * Counts the {@code if}, {@code else if}, {@code else} and loop lines, prints each count, and
     * returns their sum plus one.
     *
     * <p>{@code else if} lines are counted as {@code else}, so the printed {@code elsifs} count is
     * always 0. The total is unaffected.
     *
     * @param lines the source lines to scan
     * @return the cyclomatic complexity
     */
    public static int calculate(List<String> lines) {
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

        System.out.println("ifs: " + ifs);
        System.out.println("elsifs: " + elseIfs);
        System.out.println("els: " + elses);
        System.out.println("loops: " + loops);
        return ifs + elseIfs + elses + loops + 1;
    }
}
