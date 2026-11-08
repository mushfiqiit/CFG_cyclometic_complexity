package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.ConditionSite.Requirement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Finds the {@code if}, {@code else if} and {@code while} conditions of a function and what must
 * hold to reach each one. {@code for} loops are assumed to run their body at least once.
 */
final class ConditionPaths {

    /** The kinds of block that can enclose a condition. */
    private enum Kind { FUNCTION, IF, ELSE_IF, ELSE, LOOP }

    /**
     * An open block.
     *
     * @param kind what opened the block
     * @param inside what holds for every line inside the block
     * @param chain for an {@code if} or {@code else if}: what holds when the chain reaches the
     *        next {@code else}, meaning this condition and the earlier ones in the chain are false
     */
    private record Block(Kind kind, List<Requirement> inside, List<Requirement> chain) {
    }

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
     * @param code the source lines with comments removed, in the supported layout
     * @return the conditions with their paths
     */
    static List<ConditionSite> find(List<String> code) {
        List<ConditionSite> sites = new ArrayList<>();
        Deque<Block> blocks = new ArrayDeque<>();
        List<Requirement> closedChain = List.of();
        boolean inFunction = false;

        for (int index = 0; index < code.size(); index++) {
            String line = code.get(index).strip();
            int lineNumber = index + 1;
            if (line.isEmpty() || (!inFunction && line.startsWith("#"))) {
                continue;
            }
            if (!inFunction) {
                inFunction = true;
                blocks.push(new Block(Kind.FUNCTION, List.of(), List.of()));
                if (!isConditionalBlock(line)) {
                    continue;
                }
            }
            if (blocks.isEmpty()) {
                break;
            }
            List<Requirement> earlier = closedChain;
            closedChain = List.of();
            List<Requirement> context = context(blocks);

            if (line.equals("}")) {
                Block closed = blocks.pop();
                if (closed.kind() == Kind.IF || closed.kind() == Kind.ELSE_IF) {
                    closedChain = closed.chain();
                }
            } else if (SyntaxIdentifier.isElseIf(line)) {
                Condition condition = Condition.from(line);
                sites.add(new ConditionSite(lineNumber, condition, join(context, earlier)));
                blocks.push(new Block(Kind.ELSE_IF,
                        join(earlier, List.of(new Requirement(lineNumber, condition, true))),
                        join(earlier, List.of(new Requirement(lineNumber, condition, false)))));
            } else if (SyntaxIdentifier.isElse(line)) {
                blocks.push(new Block(Kind.ELSE, earlier, List.of()));
            } else if (SyntaxIdentifier.isIf(line)) {
                Condition condition = Condition.from(line);
                sites.add(new ConditionSite(lineNumber, condition, context));
                blocks.push(new Block(Kind.IF, List.of(new Requirement(lineNumber, condition, true)),
                        List.of(new Requirement(lineNumber, condition, false))));
            } else if (SyntaxIdentifier.isWhile(line)) {
                Condition condition = Condition.from(line);
                sites.add(new ConditionSite(lineNumber, condition, context));
                blocks.push(new Block(Kind.LOOP, List.of(new Requirement(lineNumber, condition, true)), List.of()));
            } else if (SyntaxIdentifier.isFor(line)) {
                blocks.push(new Block(Kind.LOOP, List.of(), List.of()));
            }
        }
        return sites;
    }

    // ***************************************************************************
    // Helpers
    // ***************************************************************************

    /**
     * Tells whether a line opens a block that this class tracks.
     *
     * @param line the stripped line
     * @return true for {@code if}, {@code else}, {@code for} and {@code while} lines
     */
    private static boolean isConditionalBlock(String line) {
        return SyntaxIdentifier.isIf(line) || SyntaxIdentifier.isElse(line) || SyntaxIdentifier.isLoop(line);
    }

    /**
     * Collects what holds inside every open block, from the outermost to the innermost.
     *
     * @param blocks the open blocks, innermost first
     * @return the requirements that lead to the current line
     */
    private static List<Requirement> context(Deque<Block> blocks) {
        List<Requirement> context = new ArrayList<>();
        Iterator<Block> outermostFirst = blocks.descendingIterator();
        while (outermostFirst.hasNext()) {
            context.addAll(outermostFirst.next().inside());
        }
        return context;
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
