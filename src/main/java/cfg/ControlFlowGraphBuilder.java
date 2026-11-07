package cfg;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link ControlFlowGraph} from C source lines, with one node per non-blank line.
 *
 * <p>The source is expected to put each statement on its own line, open each block at the end
 * of its {@code if}, {@code else if}, {@code else}, {@code for} or {@code while} line, and close
 * it with a <code>}</code> line. Create a builder with {@link #from(List)}, which locates the root,
 * then call {@link #build()} once to link the remaining lines.
 *
 * @author sikde
 */
public final class ControlFlowGraphBuilder {

    private final List<String> lines;
    private final Node root;
    private int nextLine;
    private boolean built;

    /**
     * Creates a builder whose root is the given line. Use {@link #from(List)} to create builders.
     *
     * @param lines the source lines; each line's index becomes its node number
     * @param rootLine index of the line to use as the root
     */
    private ControlFlowGraphBuilder(List<String> lines, int rootLine) {
        this.lines = lines;
        this.root = new Node(rootLine, lines.get(rootLine));
        this.nextLine = rootLine + 1;
    }

    // ***************************************************************************
    // Creating a builder
    // ***************************************************************************

    /**
     * Creates a builder for a source file, skipping blank and preamble lines and using the first
     * line after them as the root. That line is normally the {@code main} function's opening line.
     *
     * @param lines the source lines; each line's index becomes its node number
     * @return a builder that has found the root but not linked any other lines yet
     * @throws IndexOutOfBoundsException if every line is blank or preamble
     */
    public static ControlFlowGraphBuilder from(List<String> lines) {
        int rootLine = 0;
        while (isPreamble(lines.get(rootLine))) {
            rootLine++;
        }
        return new ControlFlowGraphBuilder(lines, rootLine);
    }

    /**
     * Tells whether a line comes before the code to graph: a blank line, a line containing
     * {@code intmain()}, or one starting with {@code #} or {@code /}.
     *
     * @param line the source line to check
     * @return true if the line is part of the preamble
     */
    private static boolean isPreamble(String line) {
        return line.isBlank() || line.contains("intmain(){") || line.charAt(0) == '#' || line.charAt(0) == '/';
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
     * @throws ArrayIndexOutOfBoundsException if a reachable node's number is
     *         {@link GraphData#MAX_NODES} or more
     */
    public ControlFlowGraph build() {
        if (built) {
            throw new IllegalStateException("build() can only be called once");
        }
        built = true;
        buildBlock(List.of(root));
        return new ControlFlowGraph(root, lines);
    }

    /**
     * Links the statements of a block, starting at the next line, until the line that closes the
     * block or the end of the file. Blank lines are skipped.
     *
     * @param entries the nodes that lead into the block's first statement
     * @return the nodes that lead out of the block: its closing line, or the last statements if
     *         the file ends before the block is closed
     */
    private List<Node> buildBlock(List<Node> entries) {
        List<Node> previous = entries;
        while (nextLine < lines.size()) {
            String statement = lines.get(nextLine);
            if (statement.isBlank()) {
                nextLine++;
            } else if (SyntaxIdentifier.endsBlock(statement)) {
                Node closing = readNode();
                link(previous, closing);
                return List.of(closing);
            } else if (SyntaxIdentifier.isIf(statement) || SyntaxIdentifier.isElse(statement)) {
                previous = buildIfChain(previous);
            } else if (SyntaxIdentifier.isLoop(statement)) {
                previous = buildLoop(previous);
            } else {
                Node node = readNode();
                link(previous, node);
                previous = List.of(node);
            }
        }
        return previous;
    }

    /**
     * Links an {@code if} statement with its {@code else if} and {@code else} branches. Each
     * condition leads to its own block and, when false, to the next branch. If the chain has no
     * {@code else}, the last condition also leads to the statement after the chain.
     *
     * @param entries the nodes that lead into the first condition
     * @return the nodes that lead to the statement after the chain
     */
    private List<Node> buildIfChain(List<Node> entries) {
        Node condition = readNode();
        link(entries, condition);
        List<Node> exits = new ArrayList<>(buildBlock(List.of(condition)));

        Node falseBranch = condition;
        while (falseBranch != null && nextNonBlankIsElse()) {
            Node branch = readNode();
            falseBranch.addChild(branch);
            exits.addAll(buildBlock(List.of(branch)));
            falseBranch = SyntaxIdentifier.isElseIf(branch.getStatement()) ? branch : null;
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
     * @param entries the nodes that lead into the loop
     * @return the loop node, which leads to the statement after the loop
     */
    private List<Node> buildLoop(List<Node> entries) {
        Node loop = readNode();
        link(entries, loop);
        for (Node bodyExit : buildBlock(List.of(loop))) {
            if (bodyExit != loop) {
                bodyExit.addChild(loop);
            }
        }
        return List.of(loop);
    }

    // ***************************************************************************
    // Helpers
    // ***************************************************************************

    /**
     * Creates the node for the next line and moves past it.
     *
     * @return the new node
     */
    private Node readNode() {
        Node node = new Node(nextLine, lines.get(nextLine));
        nextLine++;
        return node;
    }

    /**
     * Skips blank lines and tells whether the next line continues an if chain.
     *
     * @return true if the next non-blank line starts with {@code else}
     */
    private boolean nextNonBlankIsElse() {
        while (nextLine < lines.size() && lines.get(nextLine).isBlank()) {
            nextLine++;
        }
        return nextLine < lines.size() && SyntaxIdentifier.isElse(lines.get(nextLine));
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
