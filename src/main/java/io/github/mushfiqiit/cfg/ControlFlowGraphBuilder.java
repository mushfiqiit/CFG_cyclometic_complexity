package io.github.mushfiqiit.cfg;

import io.github.mushfiqiit.cfg.SourceTree.Block;
import io.github.mushfiqiit.cfg.SourceTree.Branch;
import io.github.mushfiqiit.cfg.SourceTree.Function;
import io.github.mushfiqiit.cfg.SourceTree.IfChain;
import io.github.mushfiqiit.cfg.SourceTree.Line;
import io.github.mushfiqiit.cfg.SourceTree.Loop;
import io.github.mushfiqiit.cfg.SourceTree.Simple;
import io.github.mushfiqiit.cfg.SourceTree.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link ControlFlowGraph} from C source lines, with one node per non-blank line.
 *
 * <p>The lines are first read into a {@link SourceTree} by {@link SourceParser}; the graph is then
 * linked by walking that tree. Create a builder with {@link #from(List)}, which reads the
 * structure and creates the root, then call {@link #build()} once to link the remaining lines.
 *
 * @author sikde
 */
public final class ControlFlowGraphBuilder {

    private final List<String> lines;
    private final Function function;
    private final Node root;
    private boolean built;

    /**
     * Creates a builder for a function that has already been read.
     *
     * @param lines the source lines; each line's index becomes its node number
     * @param function the function's structure
     */
    private ControlFlowGraphBuilder(List<String> lines, Function function) {
        this.lines = lines;
        this.function = function;
        this.root = nodeFor(function.header());
    }

    // ***************************************************************************
    // Creating a builder
    // ***************************************************************************

    /**
     * Creates a builder for a source file. The first line that is not blank or preamble becomes
     * the root; it is normally the {@code main} function's opening line.
     *
     * @param lines the source lines; each line's index becomes its node number
     * @return a builder that has found the root but not linked any other lines yet
     * @throws IllegalArgumentException if every line is blank or preamble
     */
    public static ControlFlowGraphBuilder from(List<String> lines) {
        Function function = SourceParser.parse(lines)
                .orElseThrow(() -> new IllegalArgumentException("there is no code to build a graph from"));
        return new ControlFlowGraphBuilder(lines, function);
    }

    // ***************************************************************************
    // Building the graph
    // ***************************************************************************

    /**
     * Returns the root node, which is known before {@link #build()} runs.
     *
     * @return the root node
     */
    public Node root() {
        return root;
    }

    /**
     * Links every line of the root's block and returns the finished graph.
     *
     * @return the control flow graph
     * @throws IllegalStateException if called more than once
     */
    public ControlFlowGraph build() {
        if (built) {
            throw new IllegalStateException("build() can only be called once");
        }
        built = true;
        linkBlock(function.body(), List.of(root));
        return new ControlFlowGraph(root, lines);
    }

    /**
     * Links a block's statements in order, then its closing line.
     *
     * @param block the block to link
     * @param entries the nodes that lead into the block's first statement
     * @return the nodes that lead out of the block: its closing line, or the last statements if
     *         the file ends before the block is closed
     */
    private static List<Node> linkBlock(Block block, List<Node> entries) {
        List<Node> previous = entries;
        for (Statement statement : block.statements()) {
            previous = linkStatement(statement, previous);
        }
        if (block.closing().isEmpty()) {
            return previous;
        }
        Node closing = nodeFor(block.closing().get());
        link(previous, closing);
        return List.of(closing);
    }

    /**
     * Links one statement after the nodes that lead into it.
     *
     * @param statement the statement to link
     * @param entries the nodes that lead into it
     * @return the nodes that lead to the statement after it
     */
    private static List<Node> linkStatement(Statement statement, List<Node> entries) {
        if (statement instanceof IfChain chain) {
            return linkIfChain(chain, entries);
        }
        if (statement instanceof Loop loop) {
            return linkLoop(loop, entries);
        }
        Node node = nodeFor(((Simple) statement).line());
        link(entries, node);
        return List.of(node);
    }

    /**
     * Links an {@code if} chain. Each condition leads to its own block and, when false, to the
     * next branch. If the chain has no {@code else}, the last condition also leads to the
     * statement after the chain.
     *
     * @param chain the chain to link
     * @param entries the nodes that lead into the first condition
     * @return the nodes that lead to the statement after the chain
     */
    private static List<Node> linkIfChain(IfChain chain, List<Node> entries) {
        List<Node> exits = new ArrayList<>();
        Node falseBranch = null;
        for (Branch branch : chain.branches()) {
            Node header = nodeFor(branch.header());
            if (falseBranch == null) {
                link(entries, header);
            } else {
                falseBranch.addChild(header);
            }
            exits.addAll(linkBlock(branch.body(), List.of(header)));
            boolean isCondition = falseBranch == null || SyntaxIdentifier.isElseIf(branch.header().text());
            falseBranch = isCondition ? header : null;
        }
        if (falseBranch != null) {
            exits.add(falseBranch);
        }
        return exits;
    }

    /**
     * Links a {@code for} or {@code while} loop: the loop leads to its block, the end of the block
     * leads back to the loop, and the loop leads to the statement after it when it finishes.
     *
     * @param loop the loop to link
     * @param entries the nodes that lead into the loop
     * @return the loop node, which leads to the statement after the loop
     */
    private static List<Node> linkLoop(Loop loop, List<Node> entries) {
        Node header = nodeFor(loop.header());
        link(entries, header);
        for (Node bodyExit : linkBlock(loop.body(), List.of(header))) {
            if (bodyExit != header) {
                bodyExit.addChild(header);
            }
        }
        return List.of(header);
    }

    // ***************************************************************************
    // Helpers
    // ***************************************************************************

    /**
     * Creates the node for a line.
     *
     * @param line the line
     * @return a node numbered by the line's index
     */
    private static Node nodeFor(Line line) {
        return new Node(line.index(), line.text());
    }

    /**
     * Makes every node in {@code from} lead to {@code to}.
     *
     * @param from the nodes to link from
     * @param to the node to link to
     */
    private static void link(List<Node> from, Node to) {
        for (Node node : from) {
            node.addChild(to);
        }
    }
}
