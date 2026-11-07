package cfg;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * Builds the control flow graph of a C source file, reports its complexity and displays it.
 *
 * @author sikde
 */
public class ControlFlowGraph {

    private final List<String> lines;
    private final boolean[] visited;
    private final int[][] adjacency = new int[GraphData.MAX_NODES][GraphData.MAX_NODES];
    private int nextLine = 0;

    /**
     * Creates a graph builder for a source file. Nothing is built until {@link #analyze()} runs.
     *
     * @param lines the source lines; each line's index becomes its node number
     */
    public ControlFlowGraph(List<String> lines) {
        this.lines = lines;
        this.visited = new boolean[lines.size()];
    }

    // ***************************************************************************
    // Running the analysis
    // ***************************************************************************

    /**
     * Builds the graph, writes the levels and edges files, prints the graph and both complexity
     * results, then opens the graph window and blocks until it is closed.
     *
     * <p>Blank lines and files longer than {@link GraphData#MAX_NODES} lines make this method
     * throw a runtime exception.
     *
     * @throws IOException if either graph file cannot be written
     */
    public void analyze() throws IOException {
        Node root = findRoot();
        System.out.println("\n\n\nroot node no: " + root.getNumber() + "\n"
                + "root node statement: " + root.getStatement());

        buildBranch(root, false);
        fillAdjacency(root);
        saveLevels(computeLevels(root));
        printGraph();
        saveEdges();

        System.out.println("Cyclometic complexity is " + ComplexityCalculator.calculate(lines));
        GraphViewer.show();
    }

    // ***************************************************************************
    // Building the graph from source lines
    // ***************************************************************************

    /**
     * Skips the preamble lines and creates the node for the first line after them, which is
     * normally the {@code main} function's opening line.
     *
     * @return the root node of the graph
     * @throws IndexOutOfBoundsException if every line is preamble
     */
    private Node findRoot() {
        while (isPreamble(lines.get(nextLine))) {
            nextLine++;
        }
        Node root = new Node(nextLine, lines.get(nextLine));
        nextLine++;
        return root;
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

    // ***************************************************************************
    // Traversing the graph
    // ***************************************************************************

    /**
     * Records every edge reachable from {@code node} in the adjacency matrix, using a depth-first
     * traversal.
     *
     * @param node the node to start from
     */
    private void fillAdjacency(Node node) {
        visited[node.getNumber()] = true;

        for (Node child : node.getChildren()) {
            if (!visited[child.getNumber()]) {
                fillAdjacency(child);
            }
        }
        for (Node child : node.getChildren()) {
            adjacency[node.getNumber()][child.getNumber()] = 1;
        }
    }

    /**
     * Finds each node's distance from the root with a breadth-first traversal. The root is on
     * level 1.
     *
     * @param root the node to measure from
     * @return each node's level, indexed by node number; {@link GraphData#UNREACHABLE_LEVEL} for
     *         nodes the root cannot reach
     */
    private int[] computeLevels(Node root) {
        int[] levels = new int[GraphData.MAX_NODES];
        Arrays.fill(levels, GraphData.UNREACHABLE_LEVEL);
        levels[root.getNumber()] = 1;

        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(root.getNumber());
        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (int next = 0; next < GraphData.MAX_NODES; next++) {
                if (hasEdge(current, next) && levels[next] > levels[current] + 1) {
                    levels[next] = levels[current] + 1;
                    queue.add(next);
                }
            }
        }
        return levels;
    }

    // ***************************************************************************
    // Querying the adjacency matrix
    // ***************************************************************************

    /**
     * Tells whether the graph has an edge between two nodes.
     *
     * @param from number of the node the edge leaves
     * @param to number of the node the edge enters
     * @return true if the edge exists
     */
    private boolean hasEdge(int from, int to) {
        return adjacency[from][to] == 1;
    }

    /**
     * Counts the edges between nodes for the source lines.
     *
     * @return the number of edges
     */
    private int countEdges() {
        int edgeCount = 0;
        for (int from = 0; from < lines.size(); from++) {
            for (int to = 0; to < lines.size(); to++) {
                if (hasEdge(from, to)) {
                    edgeCount++;
                }
            }
        }
        return edgeCount;
    }

    // ***************************************************************************
    // Saving the graph to files
    // ***************************************************************************

    /**
     * Writes the levels file: the line count, then one {@code "<node> <level>"} line per node.
     *
     * @param levels each node's level, indexed by node number
     * @throws IOException if the file cannot be written
     */
    private void saveLevels(int[] levels) throws IOException {
        try (FileWriter writer = new FileWriter(GraphData.LEVELS_FILE)) {
            writer.write(lines.size() + "\n");
            for (int i = 0; i < lines.size(); i++) {
                writer.write(i + " " + levels[i] + "\n");
            }
        }
    }

    /**
     * Writes the edges file, one {@code "<from> <to>"} line per edge.
     *
     * @throws IOException if the file cannot be written
     */
    private void saveEdges() throws IOException {
        try (FileWriter writer = new FileWriter(GraphData.EDGES_FILE)) {
            for (int from = 0; from < lines.size(); from++) {
                for (int to = 0; to < lines.size(); to++) {
                    if (hasEdge(from, to)) {
                        writer.write(from + " " + to + "\n");
                    }
                }
            }
        }
    }

    // ***************************************************************************
    // Printing the graph to the console
    // ***************************************************************************

    /** Prints the adjacency list, the adjacency matrix and the graph-based complexity. */
    private void printGraph() {
        printAdjacencyList();
        printAdjacencyMatrix();
        printGraphComplexity();
    }

    /** Prints each node's number followed by the numbers of the nodes it leads to. */
    private void printAdjacencyList() {
        System.out.println("\nAdjacency List:");
        for (int from = 0; from < lines.size(); from++) {
            System.out.print("\t" + from + "  ->   ");
            for (int to = 0; to < lines.size(); to++) {
                if (hasEdge(from, to)) {
                    System.out.print(to + " ");
                }
            }
            System.out.println();
        }
    }

    /** Prints the adjacency matrix, one row per node, with 1 marking an edge. */
    private void printAdjacencyMatrix() {
        System.out.println("\nAdjacency Matrix:");
        for (int from = 0; from < lines.size(); from++) {
            System.out.print("\t" + from + "\t");
            for (int to = 0; to < lines.size(); to++) {
                System.out.print(adjacency[from][to] + " ");
            }
            System.out.println();
        }
    }

    /**
     * Prints the node and edge counts and the complexity calculated from them as
     * edges - nodes + 2. The node count is the number of lines minus one.
     *
     * <p>The "Edges" and "Verteces" labels are swapped: each shows the other's count.
     */
    private void printGraphComplexity() {
        int nodeCount = lines.size() - 1;
        int edgeCount = countEdges();

        System.out.println("Edges " + nodeCount);
        System.out.println("Verteces " + edgeCount);
        System.out.println("Cyclometic complexity using cfg is " + (edgeCount - nodeCount + 2));
    }
}
