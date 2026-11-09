package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.SourceTree.Branch;
import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Statement;

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
        return source.statements().stream()
                .flatMap(Statement::withNested)
                .map(ComplexityCalculator::decisionsIn)
                .reduce(DecisionCounts.NONE, DecisionCounts::plus);
    }

    /**
     * Counts the decision points a single statement opens, not counting the statements nested
     * inside it.
     *
     * @param statement the statement
     * @return one loop for a loop, the {@code if} and {@code else if} branches of an {@code if}
     *         chain, or nothing for a simple statement
     */
    private static DecisionCounts decisionsIn(Statement statement) {
        if (statement instanceof Loop) {
            return new DecisionCounts(0, 0, 1);
        }
        if (statement instanceof IfChain chain) {
            return chain.branches().stream()
                    .map(branch -> decisionAt(branch.kind()))
                    .reduce(DecisionCounts.NONE, DecisionCounts::plus);
        }
        return DecisionCounts.NONE;
    }

    /**
     * Counts the decision point an {@code if} chain branch opens.
     *
     * @param kind the branch's kind
     * @return one {@code if}, one {@code else if}, or nothing for a plain {@code else}
     */
    private static DecisionCounts decisionAt(Branch.Kind kind) {
        return switch (kind) {
            case IF -> new DecisionCounts(1, 0, 0);
            case ELSE_IF -> new DecisionCounts(0, 1, 0);
            case ELSE -> DecisionCounts.NONE;
        };
    }
}
