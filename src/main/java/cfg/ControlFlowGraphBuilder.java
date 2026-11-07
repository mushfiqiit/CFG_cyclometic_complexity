package cfg;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link ControlFlowGraph} from C source lines, with one node per line.
 *
 * <p>Create a builder with {@link #from(List)}, which locates the root, then call
 * {@link #build()} once to link the remaining lines.
 *
 * @author sikde
 */
public final class ControlFlowGraphBuilder {

    private final List<String> lines;
    private final Node root;
    private int nextLine;

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
     * Creates a builder for a source file, skipping the preamble lines and using the first line
     * after them as the root. That line is normally the {@code main} function's opening line.
     *
     * @param lines the source lines; each line's index becomes its node number
     * @return a builder that has found the root but not linked any other lines yet
     * @throws IndexOutOfBoundsException if every line is preamble
     * @throws StringIndexOutOfBoundsException if a preamble line is empty
     */
    public static ControlFlowGraphBuilder from(List<String> lines) {
        int rootLine = 0;
        while (isPreamble(lines.get(rootLine))) {
            rootLine++;
        }
        return new ControlFlowGraphBuilder(lines, rootLine);
    }

    /**
     * Tells whether a line comes before the code to graph: a line containing {@code intmain()},
     * or one starting with {@code #} or {@code /}.
     *
     * @param line the source line to check
     * @return true if the line is part of the preamble
     * @throws StringIndexOutOfBoundsException if the line is empty
     */
    private static boolean isPreamble(String line) {
        return line.contains("intmain(){") || line.charAt(0) == '#' || line.charAt(0) == '/';
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
     * Links every line after the root and returns the finished graph. Call this only once.
     *
     * @return the control flow graph
     * @throws StringIndexOutOfBoundsException if a line after the root is empty
     * @throws ArrayIndexOutOfBoundsException if a reachable node's number is
     *         {@link GraphData#MAX_NODES} or more
     */
    public ControlFlowGraph build() {
        buildBranch(root, false);
        return new ControlFlowGraph(root, lines.size());
    }

    /**
     * Links the lines after {@code branchRoot} until its block closes.
     *
     * @param branchRoot the line that opens the block
     * @param inLoop whether {@code branchRoot} is a loop, so the last line links back to it
     * @return the node that closes the block, or null if the file ends first
     */
    private Node buildBranch(Node branchRoot, boolean inLoop) {
        Node previous = branchRoot;
        List<Node> openBranchEnds = new ArrayList<>();

        while (nextLine < lines.size()) {
            Node node = new Node(nextLine, lines.get(nextLine));
            String statement = node.getStatement();
            nextLine++;

            if (SyntaxIdentifier.isElse(statement)) {
                previous.addChild(node);
                openBranchEnds.add(buildBranch(node, false));
            } else if (SyntaxIdentifier.isIf(statement)) {
                linkFirstOpenBranch(openBranchEnds, previous, node);
                openBranchEnds.add(buildBranch(node, false));
            } else if (SyntaxIdentifier.isLoop(statement)) {
                linkFirstOpenBranch(openBranchEnds, previous, node);
                openBranchEnds.add(node);
                buildBranch(node, true);
            } else {
                linkAllOpenBranches(openBranchEnds, previous, node);
                if (SyntaxIdentifier.endsBlock(statement)) {
                    if (inLoop) {
                        node.addChild(branchRoot);
                    }
                    return node;
                }
                previous = node;
            }
        }
        return null;
    }

    /**
     * Links {@code node} after the previous line, or after the first open branch if there are any.
     * Any other open branches are dropped without being linked.
     *
     * @param openBranchEnds the last nodes of branches still waiting for a successor; cleared here
     * @param previous the line before {@code node}, used when no branch is open
     * @param node the line to link
     */
    private static void linkFirstOpenBranch(List<Node> openBranchEnds, Node previous, Node node) {
        if (openBranchEnds.isEmpty()) {
            previous.addChild(node);
        } else {
            openBranchEnds.get(0).addChild(node);
            openBranchEnds.clear();
        }
    }

    /**
     * Links {@code node} after every open branch, or after the previous line if none is open.
     *
     * @param openBranchEnds the last nodes of branches still waiting for a successor; cleared here
     * @param previous the line before {@code node}, used when no branch is open
     * @param node the line to link
     */
    private static void linkAllOpenBranches(List<Node> openBranchEnds, Node previous, Node node) {
        if (openBranchEnds.isEmpty()) {
            previous.addChild(node);
        } else {
            for (Node branchEnd : openBranchEnds) {
                branchEnd.addChild(node);
            }
            openBranchEnds.clear();
        }
    }
}
