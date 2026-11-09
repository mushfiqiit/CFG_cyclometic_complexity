package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.SourceTree.Block;
import io.github.mushfiqiit.cfg.SourceTree.Branch;
import io.github.mushfiqiit.cfg.SourceTree.Function;
import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Simple;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Links the lines of a parsed function into the edges of its control flow graph. Each line is
 * identified by its index in the file. Use {@link ControlFlowGraph#of(ParsedSource)}.
 */
final class ControlFlowGraphBuilder {

    private final List<SortedSet<Integer>> successors;

    /**
     * Creates a builder with no edges yet.
     *
     * @param lineCount the number of lines in the source file
     */
    private ControlFlowGraphBuilder(int lineCount) {
        successors = new ArrayList<>(lineCount);
        for (int line = 0; line < lineCount; line++) {
            successors.add(new TreeSet<>());
        }
    }

    // ***************************************************************************
    // Building the graph
    // ***************************************************************************

    /**
     * Builds the control flow graph of a source file, starting from the function's header.
     *
     * @param source the source to build the graph of
     * @return the graph
     */
    static ControlFlowGraph build(ParsedSource source) {
        Function function = source.function();
        ControlFlowGraphBuilder builder = new ControlFlowGraphBuilder(source.lines().size());
        int entry = function.header().index();
        builder.linkBlock(function.body(), List.of(entry));
        return new ControlFlowGraph(source.lines(), entry, builder.successors);
    }

    /**
     * Links the statements of a block in order, then the block's closing line.
     *
     * @param block the block to link
     * @param entries the lines that lead into the block's first statement
     * @return the lines that lead to whatever follows the block
     */
    private List<Integer> linkBlock(Block block, List<Integer> entries) {
        List<Integer> previous = entries;
        for (Statement statement : block.statements()) {
            previous = linkStatement(statement, previous);
        }
        if (!(block instanceof Block.Closed closed)) {
            return previous;
        }
        int closing = closed.closingLine().index();
        link(previous, closing);
        return List.of(closing);
    }

    /**
     * Links one statement after the lines that lead into it.
     *
     * @param statement the statement to link
     * @param entries the lines that lead into it
     * @return the lines that lead to the statement after it
     */
    private List<Integer> linkStatement(Statement statement, List<Integer> entries) {
        if (statement instanceof IfChain chain) {
            return linkIfChain(chain, entries);
        }
        if (statement instanceof Loop loop) {
            return linkLoop(loop, entries);
        }
        int line = ((Simple) statement).line().index();
        link(entries, line);
        return List.of(line);
    }

    /**
     * Links an {@code if} chain. Each condition leads to its own block and, when false, to the
     * next branch. If the chain has no {@code else}, the last condition also leads to the
     * statement after the chain.
     *
     * @param chain the chain to link
     * @param entries the lines that lead into the first condition
     * @return the lines that lead to the statement after the chain
     */
    private List<Integer> linkIfChain(IfChain chain, List<Integer> entries) {
        List<Integer> exits = new ArrayList<>();
        List<Integer> leadingIn = entries;
        for (Branch branch : chain.branches()) {
            int header = branch.header().index();
            link(leadingIn, header);
            exits.addAll(linkBlock(branch.body(), List.of(header)));
            leadingIn = branch.kind().hasCondition() ? List.of(header) : List.of();
        }
        exits.addAll(leadingIn);
        return exits;
    }

    /**
     * Links a {@code for} or {@code while} loop: the loop leads to its block, the end of the block
     * leads back to the loop, and the loop leads to the statement after it when it finishes.
     *
     * @param loop the loop to link
     * @param entries the lines that lead into the loop
     * @return the loop line, which leads to the statement after the loop
     */
    private List<Integer> linkLoop(Loop loop, List<Integer> entries) {
        int header = loop.header().index();
        link(entries, header);
        for (int bodyExit : linkBlock(loop.body(), List.of(header))) {
            if (bodyExit != header) {
                successors.get(bodyExit).add(header);
            }
        }
        return List.of(header);
    }

    /**
     * Makes every line in {@code from} lead to {@code to}.
     *
     * @param from the lines to link from
     * @param to the line to link to
     */
    private void link(List<Integer> from, int to) {
        for (int line : from) {
            successors.get(line).add(to);
        }
    }
}
