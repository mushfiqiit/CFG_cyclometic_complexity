package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.SourceTree.Branch;
import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import java.util.List;

/** Counts the decision points in a function, which gives the cyclomatic complexity. */
public final class ComplexityCalculator {

    /** Prevents instantiation; this class only has static methods. */
    private ComplexityCalculator() {
    }

    /**
     * Counts the {@code if}, {@code else if} and loop statements. Plain {@code else} branches are
     * not counted, because they are not decision points.
     *
     * @param source the source to scan
     * @return the counts, whose {@link DecisionCounts#complexity()} is the cyclomatic complexity
     */
    static DecisionCounts countDecisions(ParsedSource source) {
        return count(source.statements());
    }

    /**
     * Counts the decision points in a list of statements, including the blocks inside them.
     *
     * @param statements the statements to scan
     * @return the counts
     */
    private static DecisionCounts count(List<Statement> statements) {
        DecisionCounts total = DecisionCounts.NONE;
        for (Statement statement : statements) {
            if (statement instanceof IfChain chain) {
                for (Branch branch : chain.branches()) {
                    total = total.plus(decisionAt(branch.header().text())).plus(count(branch.body().statements()));
                }
            } else if (statement instanceof Loop loop) {
                total = total.plus(new DecisionCounts(0, 0, 1)).plus(count(loop.body().statements()));
            }
        }
        return total;
    }

    /**
     * Counts the decision point an {@code if} chain branch opens.
     *
     * @param header the branch's {@code if}, {@code else if} or {@code else} line
     * @return one {@code if}, one {@code else if}, or nothing for a plain {@code else}
     */
    private static DecisionCounts decisionAt(String header) {
        if (SyntaxIdentifier.isElseIf(header)) {
            return new DecisionCounts(0, 1, 0);
        }
        return SyntaxIdentifier.isIf(header) ? new DecisionCounts(1, 0, 0) : DecisionCounts.NONE;
    }
}
