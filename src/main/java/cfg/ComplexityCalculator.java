package cfg;

import java.util.List;

/** Calculates cyclomatic complexity by counting branching keywords. */
public final class ComplexityCalculator {

    private ComplexityCalculator() {
    }

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
