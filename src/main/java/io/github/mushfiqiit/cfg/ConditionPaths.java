package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.ConditionSite.Requirement;
import io.github.mushfiqiit.cfg.SourceTree.Branch;
import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Line;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the {@code if}, {@code else if} and {@code while} conditions of a function and what must
 * hold to reach each one, by walking the function's {@link SourceTree}. {@code for} loops are
 * assumed to run their body at least once.
 */
final class ConditionPaths {

    /** Prevents instantiation; this class only has static methods. */
    private ConditionPaths() {
    }

    // ***************************************************************************
    // Finding conditions
    // ***************************************************************************

    /**
     * Finds every {@code if}, {@code else if} and {@code while} condition in line order, with the
     * path that leads to it.
     *
     * @param source the source to search
     * @return the conditions with their paths; empty if there is no code
     */
    static List<ConditionSite> find(ParsedSource source) {
        List<ConditionSite> sites = new ArrayList<>();
        walk(source.statements(), List.of(), sites);
        return sites;
    }

    // ***************************************************************************
    // Walking the structure
    // ***************************************************************************

    /**
     * Records the conditions in a list of statements and the blocks inside them.
     *
     * @param statements the statements, in order
     * @param path what holds when these statements run
     * @param sites where to add the conditions found
     */
    private static void walk(List<Statement> statements, List<Requirement> path, List<ConditionSite> sites) {
        for (Statement statement : statements) {
            if (statement instanceof IfChain chain) {
                walkIfChain(chain, path, sites);
            } else if (statement instanceof Loop loop) {
                walkLoop(loop, path, sites);
            }
        }
    }

    /**
     * Records the conditions of an {@code if} chain. Each {@code else if} and {@code else}
     * requires the earlier conditions in the chain to be false.
     *
     * @param chain the chain
     * @param path what holds when the chain starts
     * @param sites where to add the conditions found
     */
    private static void walkIfChain(IfChain chain, List<Requirement> path, List<ConditionSite> sites) {
        List<Requirement> earlierFalse = List.of();
        for (Branch branch : chain.branches()) {
            String header = branch.header().text();
            boolean hasCondition = SyntaxIdentifier.isElseIf(header) || !SyntaxIdentifier.isElse(header);
            if (!hasCondition) {
                walk(branch.body().statements(), join(path, earlierFalse), sites);
                return;
            }
            Condition condition = Condition.from(header);
            sites.add(new ConditionSite(branch.header().number(), condition, join(path, earlierFalse)));
            walk(branch.body().statements(), join(path, join(earlierFalse, requirement(branch.header(), condition, true))),
                    sites);
            earlierFalse = join(earlierFalse, requirement(branch.header(), condition, false));
        }
    }

    /**
     * Records a {@code while} loop's condition and the conditions in its body. A {@code for} loop
     * adds no requirement, because its condition is not analyzed.
     *
     * @param loop the loop
     * @param path what holds when the loop starts
     * @param sites where to add the conditions found
     */
    private static void walkLoop(Loop loop, List<Requirement> path, List<ConditionSite> sites) {
        if (!SyntaxIdentifier.isWhile(loop.header().text())) {
            walk(loop.body().statements(), path, sites);
            return;
        }
        Condition condition = Condition.from(loop.header().text());
        sites.add(new ConditionSite(loop.header().number(), condition, path));
        walk(loop.body().statements(), join(path, requirement(loop.header(), condition, true)), sites);
    }

    // ***************************************************************************
    // Helpers
    // ***************************************************************************

    /**
     * Creates a one-element list requiring a condition to have an outcome.
     *
     * @param line the condition's line
     * @param condition the condition
     * @param outcome the required outcome
     * @return the requirement in a list
     */
    private static List<Requirement> requirement(Line line, Condition condition, boolean outcome) {
        return List.of(new Requirement(line.number(), condition, outcome));
    }

    /**
     * Joins two lists of requirements.
     *
     * @param first the requirements that come first
     * @param second the requirements that come after them
     * @return a new read-only list with both
     */
    private static List<Requirement> join(List<Requirement> first, List<Requirement> second) {
        List<Requirement> joined = new ArrayList<>(first);
        joined.addAll(second);
        return List.copyOf(joined);
    }
}
